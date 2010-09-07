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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;

/** GameInstanceFactory tasks, e.g. create instances.
 * 
 * @author cmg
 *
 */
public class FactoryTasks implements Constants {
	static Logger logger = Logger.getLogger(FactoryUtils.class.getName());
	/** check all GameInstanceFactorys - periodic task */
	public static void checkAllGameInstanceFactories() {
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM GameInstanceFactory x ORDER BY x."+LAST_INSTANCE_CHECK_TIME+" ASC");
			List<GameInstanceFactory> gifs = (List<GameInstanceFactory>)q.getResultList();
			for (GameInstanceFactory gif : gifs) {
				checkGameInstanceFactory(gif);
			}
		}
		finally {
			em.close();
		}
	}
	/** check all GameInstanceFactorys for a GameTemplate */
	public static void checkGameInstanceFactories(GameTemplate gt) {
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM GameInstanceFactory x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" ORDER BY x."+LAST_INSTANCE_CHECK_TIME+" ASC");
			q.setParameter(GAME_TEMPLATE_ID, gt.getId());
			List<GameInstanceFactory> gifs = (List<GameInstanceFactory>)q.getResultList();
			for (GameInstanceFactory gif : gifs) {
				checkGameInstanceFactory(gif);
			}
		}
		finally {
			em.close();
		}
	}
	/**
	 * @param em
	 * @param gif
	 */
	public static void checkGameInstanceFactory(GameInstanceFactory gif) {
		if (gif.getStartTimeCron()==null) {
			// no cron...
			return;
		}
		long checkTime = gif.getLastInstanceCheckTime();
		long endTime = System.currentTimeMillis()+gif.getInstanceCreateTimeWindowMs();
		try {
			// check ahead by InstanceCreateTimeWindowMs
			if (gif.getMinTime()>checkTime) 
				checkTime = gif.getMinTime();
			// TODO starting from last check time repeatedly find the next start time,
			while (checkTime<=endTime) {
				// advance first
				checkTime = checkTime+1;
				// TODO make more efficient by cacheing parsed state
				long nextStartTime = FactoryUtils.getNextCronTime(gif.getStartTimeCron(), checkTime, gif.getMaxTime());
				if (nextStartTime<=0 || nextStartTime>gif.getMaxTime())
					// done
					break;
				// check if the instance already exists,
				// if not create it
				checkGameInstanceFactoryInstance(gif, nextStartTime);
				checkTime = nextStartTime;
			}
		} catch (CronExpressionException e) {
			// TODO Auto-generated catch block
			logger.warning("Unable to checkGameInstanceFactory "+gif.getKey()+": "+e);
		} catch (Exception e) {
			logger.log(Level.WARNING, "error checking GameInstanceFactory "+gif, e);
		}
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			// finally, atomically update the lastInstanceCheckTime (if increased)
			GameInstanceFactory ngif = em.find(GameInstanceFactory.class, gif.getKey());
			if (ngif.getLastInstanceCheckTime()<endTime)
				ngif.setLastInstanceCheckTime(endTime);
			em.merge(ngif);
			et.commit();
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}		
	}
	/**
	 * @param gif
	 * @param nextStartTime
	 */
	private static void checkGameInstanceFactoryInstance(
			GameInstanceFactory gif, long startTime) {
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
				ngi.setNominalStatus(GameInstanceNominalStatus.POSSIBLE);
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
				logger.info("Added GameInstance "+ngi);				
			}
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}		
}
}
