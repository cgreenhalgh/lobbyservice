/**
 * Copyright 2010 The University of Nottingham
 * 
 * This file is part of lobbyservice.
 *
 *  lobbyservice is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  lobbyservice is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with lobbyservice.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package uk.ac.horizon.ug.lobby.server;

import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.AuditRecordLevel;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryType;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceStatus;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateAuditRecordType;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;

/** GameInstanceFactory tasks, e.g. create instances.
 * 
 * @author cmg
 *
 */
public class FactoryTasks implements Constants {
	static Logger logger = Logger.getLogger(FactoryUtils.class.getName());
	/** check all GameInstanceFactorys - periodic task */
	public static void checkAllGameInstanceFactories() {
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM GameInstanceFactory x ORDER BY x."+LAST_INSTANCE_CHECK_TIME+" ASC");
			List<GameInstanceFactory> gifs = (List<GameInstanceFactory>)q.getResultList();
			for (GameInstanceFactory gif : gifs) {
				checkGameInstanceFactory(sc, gif);
			}
		}
		finally {
			em.close();
		}
	}
	/** check all GameInstanceFactorys for a GameTemplate */
	public static void checkGameInstanceFactories(GameTemplate gt) {
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM GameInstanceFactory x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" ORDER BY x."+LAST_INSTANCE_CHECK_TIME+" ASC");
			q.setParameter(GAME_TEMPLATE_ID, gt.getId());
			List<GameInstanceFactory> gifs = (List<GameInstanceFactory>)q.getResultList();
			for (GameInstanceFactory gif : gifs) {
				checkGameInstanceFactory(sc, gif);
			}
		}
		finally {
			em.close();
		}
	}
	/** one hour */
	public static final long ONE_HOUR = 3600000;
	/** max nominal check interval - limits rate at which tokens can be added (5 minutes) */
	public static final long MAX_CHECK_INTERVAL_MS = ONE_HOUR;
	/**
	 * @param em
	 * @param gif
	 */
	public static void checkGameInstanceFactory(ServerConfiguration sc, GameInstanceFactory gif) {
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		long now = System.currentTimeMillis();
		try {
			GameInstanceFactory ngif = em.find(GameInstanceFactory.class, gif.getKey());

			// update tokens
			long lastCheckTime = ngif.getLastInstanceCheckTime();
			if (lastCheckTime==0)
				lastCheckTime = now;
			long elapsed = now-lastCheckTime;
			if (elapsed > MAX_CHECK_INTERVAL_MS)
			{
				logger.warning("Limiting checkGameInstanceFactory elapsed to "+MAX_CHECK_INTERVAL_MS+" (was "+elapsed+"ms)");
				elapsed = MAX_CHECK_INTERVAL_MS;
			}
			// lastCheckTime offset into hour
			long lastCheckTimeHourOffsetMs = lastCheckTime % ONE_HOUR;
			int perHour = ngif.getNewInstanceTokensPerHour();
			if (perHour > sc.getMaxNewInstanceTokensPerHour())
				perHour = sc.getMaxNewInstanceTokensPerHour();
			// careful with those rounding errors, Eugene...
			int newTokens = (int)((lastCheckTimeHourOffsetMs+elapsed)*perHour/ONE_HOUR-(lastCheckTimeHourOffsetMs)*perHour/ONE_HOUR);
			// paranoid
			if (newTokens>perHour) {
				logger.warning("newTokens came out > perHour ("+newTokens+" vs "+perHour+") for "+gif);
				newTokens = perHour;
			}
			if (newTokens<0) {
				logger.warning("newTokens came out < 0 ("+newTokens+") for "+gif);
				newTokens = 0;
			}
			int tokens = ngif.getNewInstanceTokens()+newTokens;
			if (tokens > ngif.getNewInstanceTokensMax())
				tokens = ngif.getNewInstanceTokensMax();
			if (tokens > sc.getMaxNewInstanceTokensMax())
				tokens = sc.getMaxNewInstanceTokensMax();
			
			if (tokens!=ngif.getNewInstanceTokens()) {
				logger.info("Changed newInstanceTokens to "+tokens+" from "+ngif.getNewInstanceTokens()+" for "+gif.getTitle());
			}
			ngif.setNewInstanceTokens(tokens);
			ngif.setLastInstanceCheckTime(now);
			em.merge(ngif);
			et.commit();
			
			gif = ngif;
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}		

		if (gif.getType()!=GameInstanceFactoryType.SCHEDULED)
			// not scheduled
			return;
		
		if (gif.getStartTimeOptionsJson()==null) {
			// no cron...
			AuditUtils.logGameTemplateAuditRecordIfNovel(gif.getGameTemplateId(), gif.getKey(), /*gameInstanceKey*/null, /*accountKey*/null, /*clientIp*/null, System.currentTimeMillis(), GameTemplateAuditRecordType.SYSTEM_CREATE_GAME_INSTANCE_FAILED, AuditRecordLevel.WARNING, /*detailsJson*/null, "Unable to check GameInstanceFactory - no startTimeOptionsJson");
			return;
		}

		int tokensCache = gif.getNewInstanceTokens();
		long checkTime = gif.getLastInstanceStartTime();
		if (checkTime < now) {
			logger.warning("Skipping checks from lastInstanceStartTime "+checkTime+" to now ("+now+") for "+gif.getTitle());
			// TODO audit record
			checkTime = now;
		}
		if (checkTime < gif.getMinTime()-1)
			// will be advanced by one before first check!
			checkTime = gif.getMinTime()-1;
		
		long maxTime = now+gif.getInstanceCreateTimeWindowMs();
		if (gif.getMaxTime() < maxTime)
			maxTime = gif.getMaxTime();		
		
		try {
			TreeSet values[] = FactoryUtils.parseTimeOptionsJson(gif.getStartTimeOptionsJson());
			
			// starting from last check time repeatedly find the next start time,
			while (checkTime<=maxTime) {
				// advance first
				checkTime = checkTime+1;
				// TODO make more efficient by cacheing parsed state
				long nextStartTime = FactoryUtils.getNextCronTime(gif.getStartTimeCron(), values, checkTime, maxTime);
				if (nextStartTime<=0 || nextStartTime>maxTime)
					// done
					break;
				// check if the instance already exists,
				// if not create it
				tokensCache = checkGameInstanceFactoryInstance(gif, nextStartTime, tokensCache);
				if (tokensCache<0) {
					// audit
					logger.warning("GameInstanceFactory could not create instance at "+nextStartTime+" due to token limit: "+gif);
					AuditUtils.logGameTemplateAuditRecordIfNovel(gif.getGameTemplateId(), gif.getKey(), /*gameInstanceKey*/null, /*accountKey*/null, /*clientIp*/null, System.currentTimeMillis(), GameTemplateAuditRecordType.SYSTEM_CREATE_GAME_INSTANCE_FAILED, AuditRecordLevel.WARNING, /*detailsJson*/null, "Unable to create GameInstance - no tokens");
					break;
				}
				checkTime = nextStartTime;
			}
		} catch (CronExpressionException e) {
			logger.warning("Unable to checkGameInstanceFactory "+gif.getKey()+": "+e);
			AuditUtils.logGameTemplateAuditRecordIfNovel(gif.getGameTemplateId(), gif.getKey(), /*gameInstanceKey*/null, /*accountKey*/null, /*clientIp*/null, System.currentTimeMillis(), GameTemplateAuditRecordType.SYSTEM_CREATE_GAME_INSTANCE_FAILED, AuditRecordLevel.WARNING, /*detailsJson*/null, "Unable to check GameInstanceFactory ("+e.getMessage()+")");
		} catch (Exception e) {
			logger.log(Level.WARNING, "error checking GameInstanceFactory "+gif, e);
			AuditUtils.logGameTemplateAuditRecordIfNovel(gif.getGameTemplateId(), gif.getKey(), /*gameInstanceKey*/null, /*accountKey*/null, /*clientIp*/null, System.currentTimeMillis(), GameTemplateAuditRecordType.SYSTEM_CREATE_GAME_INSTANCE_FAILED, AuditRecordLevel.WARNING, /*detailsJson*/null, "Unable to check GameInstanceFactory ("+e.getMessage()+")");
		}
	}
	/**
	 * @param gif
	 * @param nextStartTime
	 * @return new value of newInstanceTokens; returns -1 to signal could not create
	 */
	private static int checkGameInstanceFactoryInstance(
			GameInstanceFactory gif, long startTime, int tokensCache) {
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			// does this instance already exist?
			Query q = em.createQuery("SELECT x FROM GameInstance x WHERE x."+GAME_INSTANCE_FACTORY_KEY+" = :"+GAME_INSTANCE_FACTORY_KEY+" AND x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" AND x."+START_TIME+" = :"+START_TIME);
			q.setParameter(GAME_INSTANCE_FACTORY_KEY, gif.getKey());
			q.setParameter(GAME_TEMPLATE_ID, gif.getGameTemplateId());
			q.setParameter(START_TIME, startTime);
			List<GameInstance> fgis = q.getResultList();
			if (fgis.size()>0) {
				GameInstance fgi = fgis.get(0);
				if (fgi.getVisibility()!=GameTemplateVisibility.PUBLIC) {
					logger.warning("GameInstance "+gif.getKey()+" / "+startTime+" exists but is "+fgi.getVisibility());
				}
				else if (fgi.getNominalStatus()==GameInstanceNominalStatus.CANCELLED || fgi.getNominalStatus()==GameInstanceNominalStatus.ENDED) {
					logger.warning("GameInstance "+gif.getKey()+" / "+startTime+" exists but is "+fgi.getNominalStatus());						
				}
			}
			else {
				if (tokensCache<=0) 
					return -1;
				
				// create GameInstance on demand?!
				GameInstance ngi = new GameInstance();
				ngi.setAllowAnonymousClients(gif.isAllowAnonymousClients());
				//ngi.setBaseUrl();
				ngi.setCreatedTime(System.currentTimeMillis());
				ngi.setEndTime(startTime+gif.getDurationMs());
				ngi.setGameInstanceFactoryKey(gif.getKey());
				ngi.setGameServerId(gif.getGameServerId());
				ngi.setGameTemplateId(gif.getGameTemplateId());
				switch(gif.getLocationType()) {
				case GLOBAL:
					ngi.setRadiusMetres(0);
					break;
				case SPECIFIED_LOCATION:
					ngi.setLatitudeE6(gif.getLatitudeE6());
					ngi.setLongitudeE6(gif.getLongitudeE6());
					break;
				}
				ngi.setLocationName(gif.getLocationName());
				ngi.setMaxNumSlots(gif.getMaxNumSlots());
				// SCHEDULED => PLANNED
				ngi.setNominalStatus(GameInstanceNominalStatus.PLANNED);
				ngi.setStatus(GameInstanceStatus.PLANNED);
				ngi.setNumSlotsAllocated(0);
				ngi.setStartTime(startTime);
				//ngi.setStatus()
				// TODO symbol subst? in title
				ngi.setTitle(gif.getInstanceTitle());
				ngi.setVisibility(gif.getInstanceVisibility());
				
				// cache
				ngi.setFull(ngi.getNumSlotsAllocated()>=ngi.getMaxNumSlots());

				em.persist(ngi);
				et.commit();
				
				// delete one from tokens
				et.begin();
				GameInstanceFactory ngif = em.find(GameInstanceFactory.class, gif.getKey());
				ngif.setNewInstanceTokens(ngif.getNewInstanceTokens()-1);
				if (startTime>ngif.getLastInstanceStartTime())
					ngif.setLastInstanceStartTime(startTime);
				else
					logger.warning("GameInstanceFactory startTime "+startTime+" before lastInstanceStartTime "+ngif.getLastInstanceStartTime());
				em.merge(ngif);
				et.commit();
				gif = ngif;
				AuditUtils.logGameTemplateAuditRecord(gif.getGameTemplateId(), gif.getKey(), ngi.getKey(), /*accountKey*/null, /*clientIp*/null, System.currentTimeMillis(), GameTemplateAuditRecordType.SYSTEM_CREATE_GAME_INSTANCE, AuditRecordLevel.NORMAL, /*detailsJson*/null, "Created GameInstance");
				//logger.info("Added GameInstance "+ngi+" (tokens="+ngif.getNewInstanceTokens()+")");		
				if (ngif.getNewInstanceTokens()>0)
					return ngif.getNewInstanceTokens();
				logger.warning("GameInstanceFactory in token-debt ("+ngif.getNewInstanceTokens()+"): "+ngif);
				return 0;
			}
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}	
		return tokensCache;
	}
}
