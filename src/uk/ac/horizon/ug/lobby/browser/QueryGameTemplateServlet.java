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
package uk.ac.horizon.ug.lobby.browser;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService; 
import com.google.appengine.api.users.UserServiceFactory; 
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.HttpUtils;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameClientType;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryLocationType;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameQuery;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.protocol.LocationConstraint;
import uk.ac.horizon.ug.lobby.protocol.TimeConstraint;
import uk.ac.horizon.ug.lobby.server.CronExpressionException;
import uk.ac.horizon.ug.lobby.server.FactoryUtils;
import uk.ac.horizon.ug.lobby.user.UserGameTemplateServlet;
import uk.me.jstott.jcoord.LatLng;

/** 
 * Get Game (templates) info, for public browsing
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class QueryGameTemplateServlet extends HttpServlet implements Constants {
	private static final double ONE_MILLION = 1000000;
	private static final double ONE_THOUSAND = 1000;
	static Logger logger = Logger.getLogger(QueryGameTemplateServlet.class.getName());

	private GameTemplate getGameTemplate(HttpServletRequest req, EntityManager em) throws RequestException {
		String id = HttpUtils.getIdFromPath(req);
		
		Key key = GameTemplate.idToKey(id);
		GameTemplate gt = em.find(GameTemplate.class, key);
        if (gt==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameTemplate "+id+" not found");
        
        return gt;
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		GameIndex gindex = sc.getGameIndex();

		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			GameQuery gq = JSONUtils.parseGameQuery(json);
			
			logger.info("GameQuery "+gq);
			GameTemplate gt = getGameTemplate(req, em);
			
			// Check GameClientTemplate for clientType, clientTitle, locationSpecific and version
			List<GameClientTemplate> posgcts = getGameClientTemplates(em, gq, gt.getId());
			logger.info("Game "+gt.getId()+" has "+posgcts.size()+" possible clients");
			// post-filter - location
			boolean locationSpecific = false;
			boolean locationIndependent = false;
			List<GameClientTemplate> gcts = new LinkedList<GameClientTemplate>();
			List<GameClientTemplate> noloc_gcts = new LinkedList<GameClientTemplate>();
			for (GameClientTemplate gct : posgcts) {
				if (gct.isLocationSpecific())
					locationSpecific = true;
				else {
					locationIndependent = true;
					noloc_gcts.add(gct);
				}
				// add gcts (if location ok)
				gcts.add(gct);
			}
			if (gcts.size()==0) {
				logger.info("Game "+gt.getId()+" does not support any client(s) specified");
				// no matching client - so can't play
				JSONUtils.sendGameIndex(resp, gindex);
				return;
			}
			logger.info("Found "+gcts.size()+" possible client templates, of which "+noloc_gcts.size()+" location-independent");
			
			// ensure GameInstanceFactorys get a chance to create relevant GameInstances...
			
			// Check GameInstance for startTime, endTime, location (if location-specific) and nominalStatus (not CANCELLED)
			Map<String,Object> qps = new HashMap<String,Object>();
			StringBuilder qb = new StringBuilder();
			//qps = new HashMap<String,Object>();
			//qb = new StringBuilder();
			qb.append("SELECT x FROM GameInstance x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" AND x."+NOMINAL_STATUS+" IN ( 'PLANNED', 'POSSIBLE', 'AVAILABLE', 'TEMPORARILY_UNAVAILABLE' ) AND x."+VISIBILITY+" = '"+GameTemplateVisibility.PUBLIC.toString()+"'");
			qps.put(GAME_TEMPLATE_ID, gt.getId());
			//qps.put(GameInstanceNominalStatus.CANCELLED.toString(), GameInstanceNominalStatus.CANCELLED);
			//qps.put(GameInstanceNominalStatus.ENDED.toString(), GameInstanceNominalStatus.ENDED);
			if (gq.getIncludeFullGames()==null || !gq.getIncludeFullGames()) {
				qb.append(" AND x."+FULL+" = FALSE");
			}
			// query constraints
			// NB GAE only allows range query on one variable - we'll use startTime for now
			if (gq.getTimeConstraint()!=null) {
				TimeConstraint tc = gq.getTimeConstraint();
				boolean usedEndTime = false, usedStartTime = false;
				if (tc.isLimitEndTime() && tc.getMaxTime()!=null) {
					// end time is limited and we have max time, so check game end time against max
					qps.put(MAX_TIME, tc.getMaxTime());
					qb.append(" AND x."+END_TIME+" <= :"+MAX_TIME);
					usedEndTime = true;
				} else if (tc.getMaxTime()!=null) {
					// end time is not limited, but we still have max time, so check start against max
					qps.put(MAX_TIME, tc.getMaxTime());
					qb.append(" AND x."+START_TIME+" <= :"+MAX_TIME);					
					usedStartTime = true;
				}
				if (!usedEndTime && !tc.isIncludeStarted() && tc.getMinTime()!=null) {
					// only consider games starting after min...
					qps.put(MIN_TIME, tc.getMinTime());
					qb.append(" AND x."+START_TIME+" >= :"+MIN_TIME);
					usedStartTime = true;
				} 
				else if (!usedStartTime && tc.isIncludeStarted() && tc.getMinTime()!=null) {
					// no limit on early start, but we must get some playing time in after our earliest start...
					if (tc.getMinDurationMs()!=null) 
						qps.put(MIN_TIME, tc.getMinTime()+tc.getMinDurationMs());
					else
						qps.put(MIN_TIME, tc.getMinTime());
					qb.append(" AND x."+END_TIME+" >= :"+MIN_TIME);
					usedEndTime = true;				
				}
				if (usedEndTime)
					qb.append(" ORDER BY x."+END_TIME+" ASC");
				else
					qb.append(" ORDER BY x."+START_TIME+" ASC");					
			}
			else
				qb.append(" ORDER BY x."+START_TIME+" ASC");					
			
			logger.info("Query: "+qb.toString());
			Query q = em.createQuery(qb.toString());
			for (String qp : qps.keySet()) {
				q.setParameter(qp, qps.get(qp));
			}
			List<GameInstance> posgis = (List<GameInstance>)q.getResultList();
			logger.info("Found "+posgis.size()+" possible GameInstances on initial query");
			
			// matching combinations
			List<GameTemplateInfo> gtis = new LinkedList<GameTemplateInfo>();
			gindex.setItems(gtis);
			
			for (GameInstance gi : posgis) {
				// recheck times (for simplicity)
				if (gq.getTimeConstraint()!=null) {
					TimeConstraint tc = gq.getTimeConstraint();
					// start time...
					if (!tc.isIncludeStarted() && tc.getMinTime()!=null) {
						if (tc.getMinTime()>gi.getStartTime()) {
							logger.info("GameInstance "+gi.getTitle()+" starts too early ("+gi.getStartTime()+" vs "+tc.getMinTime()+")");
							continue; // starts too early
						}
					} 
					// our earliest possible start time...
					long ourStart = gi.getStartTime();
					if (tc.getMinTime()!=null && tc.getMinTime()>ourStart)
						ourStart = tc.getMinTime();
					// our latest possible end time...
					long ourEnd = gi.getEndTime();
					// end time...
					if (tc.isLimitEndTime() && tc.getMaxTime()!=null) {
						if (gi.getEndTime()>tc.getMaxTime()) {
							logger.info("GameInstance "+gi.getTitle()+" end too late ("+gi.getEndTime()+" vs "+tc.getMaxTime()+")");
							continue; // ends too late
						}
					}
					// duration
					long duration = ourEnd - ourStart;
					if (duration <= 0) {
						logger.info("GameInstance "+gi.getTitle()+" would end before it starts for us ("+gi.getStartTime()+"-"+gi.getEndTime()+", min="+tc.getMinTime()+")");
						continue; // no time left
					}
					if (tc.getMinDurationMs()!=null && tc.getMinDurationMs() > duration) {
						logger.info("GameInstance "+gi.getTitle()+" too short: "+duration+" vs "+tc.getMinDurationMs());
						continue; // not long enough
					}
					if (tc.getMaxDurationMs()!=null && tc.getMaxDurationMs() < duration) {
						logger.info("GameInstance "+gi.getTitle()+" too long: "+duration+" vs "+tc.getMaxDurationMs());
						continue; // too long
					}
				}
				// location
				boolean locationOk = true;
				if (locationSpecific && gq.getLocationConstraint()!=null) {
					LocationConstraint lc = gq.getLocationConstraint();

					locationOk = checkLocationConstraint(lc, gi.getLatitudeE6(), gi.getLongitudeE6(), gi.getRadiusMetres());
				}
				if (locationOk || locationIndependent) {
					// useful
					GameTemplateInfo gti = new GameTemplateInfo();
					gti.setGameTemplate(gt);
					gti.setGameInstance(gi);
					gti.setJoinUrl(makeJoinUrl(sc, gi));
					if (locationOk)
						gti.setGameClientTemplates(gcts);
					else 
						gti.setGameClientTemplates(noloc_gcts);
					gtis.add(gti);
				}
			}

			//==================================================================================
			// now check GameInstanceTemplates...
			// Check GameInstance for startTime, endTime, location (if location-specific) and nominalStatus (not CANCELLED)
			qps = new HashMap<String,Object>();
			qb = new StringBuilder();
			//qps = new HashMap<String,Object>();
			//qb = new StringBuilder();
			qb.append("SELECT x FROM GameInstanceFactory x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" AND x."+STATUS+" = '"+GameInstanceFactoryStatus.ACTIVE.toString()+"' AND x."+VISIBILITY+" = '"+GameTemplateVisibility.PUBLIC.toString()+"'");
			qps.put(GAME_TEMPLATE_ID, gt.getId());
			// query constraints
			// NB GAE only allows range query on one variable - we'll use startTime for now
			if (gq.getTimeConstraint()!=null) {
				TimeConstraint tc = gq.getTimeConstraint();
				boolean usedMaxTime = false, usedMinTime = false;
				if (tc.getMinTime()!=null) {
					qps.put(MIN_TIME, tc.getMinTime());
					qb.append(" AND x."+MAX_TIME+" >= :"+MIN_TIME);
					usedMaxTime = true;
				} 
				else if (tc.getMaxTime()!=null) {
					qps.put(MAX_TIME, tc.getMaxTime());
					qb.append(" AND x."+MIN_TIME+" <= :"+MAX_TIME);					
					usedMinTime = true;
				}
				if (usedMaxTime)
					qb.append(" ORDER BY x."+MAX_TIME+" ASC");
				else
					qb.append(" ORDER BY x."+MIN_TIME+" ASC");					
			}
			else
				qb.append(" ORDER BY x."+MIN_TIME+" ASC");					
			
			logger.info("Query: "+qb.toString());
			//Query q
			q = em.createQuery(qb.toString());
			for (String qp : qps.keySet()) {
				q.setParameter(qp, qps.get(qp));
			}
			List<GameInstanceFactory> posgifs = (List<GameInstanceFactory>)q.getResultList();
			logger.info("Found "+posgifs.size()+" possible GameInstanceFactories on initial query");
			
			// matching combinations
			//done: List<GameTemplateInfo> gtis = new LinkedList<GameTemplateInfo>();
			
			for (GameInstanceFactory gif : posgifs) {
				// location
				boolean locationOk = true;
				// only 'SPECIFIED_LOCATION' limits at this stage
				if (locationSpecific && gq.getLocationConstraint()!=null && gif.getLocationType()==GameInstanceFactoryLocationType.SPECIFIED_LOCATION) {
					LocationConstraint lc = gq.getLocationConstraint();

					locationOk = checkLocationConstraint(lc, gif.getLatitudeE6(), gif.getLongitudeE6(), gif.getRadiusMetres());					
				}
/*				// PLAYER_LOCATION - not yet supported
				else if (gif.getLocationType()==GameInstanceFactoryLocationType.PLAYER_LOCATION) {
					if (gq.getLatitudeE6()==null || gq.getLongitudeE6()==null)
					{
						logger.warning("GameInstanceFactory "+gif.getTitle()+" is PLAYER_LOCATION - player location not provided in query");
						// can't use even if we have a locationIndependent client!
						continue;
					}
				}
*/				if (!locationOk && !locationIndependent)
					continue; // next gif
				
				// TODO PLAYER_LOCATION might limit success due to maxNumInstancesConcurrent!

				// calculate next game time from CRON pattern - check there is one and it is in range!
				if (gif.getStartTimeOptionsJson()==null) {
					logger.warning("GameTemplateFactory has no startTimeOptionsJson: "+gif);
					continue;
				}
				
				TimeConstraint tc = gq.getTimeConstraint();
				// earliest possible start time that might be relevant...
				long minTime = System.currentTimeMillis();
				if (tc!=null && tc.getMinTime()!=null && tc.getMinTime()>minTime)
					minTime = tc.getMinTime();
				if (tc!=null && tc.isIncludeStarted()) 
					// TODO long-running / variable length
					minTime = minTime-gif.getDurationMs();
				if (gif.getMinTime()>minTime)
					minTime = gif.getMinTime();
				long maxTime = Long.MAX_VALUE;
				if (tc!=null && tc.getMaxTime()!=null)
					maxTime = tc.getMaxTime();
				if (gif.getMaxTime()<maxTime)
					maxTime = gif.getMaxTime();
				
				long firstStartTime = 0;
				try {
					TreeSet values[] = FactoryUtils.parseTimeOptionsJson(gif.getStartTimeOptionsJson());
					// find fist CRON firing no earlier than that
					firstStartTime = FactoryUtils.getNextCronTime(gif.getStartTimeCron(), values, minTime, maxTime);
				} catch (CronExpressionException cee) {
					logger.warning("GameTemplateFactory error in startTimeCron: "+cee+" for "+gif);
					continue;
				}
				if (firstStartTime==0) {
					logger.warning("GameTemplateFactory has no startTime in range "+minTime+"-"+maxTime+": "+gif);
					continue;
				}
				// GameInstance code...
				if (tc!=null && tc.getMinDurationMs()!=null && tc.getMinDurationMs() > gif.getDurationMs()) {
					logger.info("GameInstanceFactory "+gif.getTitle()+" too short: "+gif.getDurationMs()+" vs "+tc.getMinDurationMs());
					continue; // not long enough
				}
				if (tc!=null && tc.getMaxDurationMs()!=null && tc.getMaxDurationMs() < gif.getDurationMs()) {
					logger.info("GameInstanceFactory "+gif.getTitle()+" too long: "+gif.getDurationMs()+" vs "+tc.getMaxDurationMs());
					continue; // too long
				}

				et.commit();
				et.begin();
				
				// TODO move game instance creation to timer thread (and check at start)
/*				// does this instance already exist?
				q = em.createQuery("SELECT x FROM GameInstance x WHERE x."+GAME_INSTANCE_FACTORY_KEY+" = :"+GAME_INSTANCE_FACTORY_KEY+" AND x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" AND x."+START_TIME+" = :"+START_TIME);
				q.setParameter(GAME_INSTANCE_FACTORY_KEY, gif.getKey());
				q.setParameter(GAME_TEMPLATE_ID, gt.getId());
				q.setParameter(START_TIME, firstStartTime);
				List<GameInstance> fgis = q.getResultList();
				if (fgis.size()>0) {
					GameInstance fgi = fgis.get(0);
					if (fgi.getVisibility()!=GameTemplateVisibility.PUBLIC) {
						logger.warning("GameInstance "+gif.getKey()+" / "+firstStartTime+" exists but is "+fgi.getVisibility());
					}
					else if (fgi.getNominalStatus()==GameInstanceNominalStatus.CANCELLED || fgi.getNominalStatus()==GameInstanceNominalStatus.ENDED) {
						logger.warning("GameInstance "+gif.getKey()+" / "+firstStartTime+" exists but is "+fgi.getNominalStatus());						
					}
					else {
						boolean found = false;
						for (GameTemplateInfo gti : gtis) {
							if (gti.getGameInstance().getKey().equals(fgi.getKey())) {
								found = true;
								break;
							}
						}
						if (!found) {
							logger.warning(""+gif.getKey()+" / "+firstStartTime+" exists but was not matched: "+fgi);
						}
						else {
							logger.info(""+gif.getKey()+" / "+firstStartTime+" exists and was matched");
						}
					}
				}
				else {
					// create GameInstance on demand?!
					GameInstance ngi = new GameInstance();
					ngi.setAllowAnonymousClients(gif.isAllowAnonymousClients());
					//ngi.setBaseUrl();
					ngi.setEndTime(firstStartTime+gif.getDurationMs());
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
					case PLAYER_LOCATION:
						ngi.setRadiusMetres(gif.getRadiusMetres());
						ngi.setLatitudeE6(gq.getLatitudeE6());
						ngi.setLongitudeE6(gq.getLongitudeE6());
						break;
					}
					ngi.setLocationName(gif.getLocationName());
					ngi.setMaxNumSlots(gif.getMaxNumSlots());
					ngi.setNominalStatus(GameInstanceNominalStatus.POSSIBLE);
					ngi.setNumSlotsAllocated(0);
					ngi.setStartTime(firstStartTime);
					//ngi.setStatus()
					// TODO symbol subst? in title
					ngi.setTitle(gif.getInstanceTitle());
					ngi.setVisibility(gif.getVisibility());
					
					// cache
					ngi.setFull(ngi.getNumSlotsAllocated()>=ngi.getMaxNumSlots());

					em.persist(ngi);
					logger.info("Added GameInstance "+ngi);
					
					et.commit();
					et.begin();

					// add to response
					GameTemplateInfo gti = new GameTemplateInfo();
					gti.setGameTemplate(gt);
					gti.setGameInstance(ngi);
					gti.setJoinUrl(makeJoinUrl(sc, ngi));
					if (locationOk)
						gti.setGameClientTemplates(gcts);
					else 
						gti.setGameClientTemplates(noloc_gcts);
					gtis.add(gti);
				}
				// end of code to move
*/			
				// GameInstanceFactory in useful itself
				GameTemplateInfo gti = new GameTemplateInfo();
				gti.setGameTemplate(gt);
				gti.setGameInstanceFactory(gif);
				try {
					gti.setGameTimeOptions(FactoryUtils.getGameTimeOptions(gif.getStartTimeCron(), firstStartTime));
				} catch (CronExpressionException e) {
					logger.warning("Generating GameTimeOptions: "+e);
				}
				gti.setFirstStartTime(firstStartTime);
				// note re-query, not join!
				gti.setQueryUrl(GetGameIndexServlet.makeQueryUrl(sc, gt));
				if (locationOk)
					gti.setGameClientTemplates(gcts);
				else 
					gti.setGameClientTemplates(noloc_gcts);
				gtis.add(gti);
				
			}
			
			// response
			JSONUtils.sendGameIndex(resp, gindex);
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		finally {	
			if (et.isActive())
				et.rollback();
			em.close();
		}
	}
	/**
	 * @param lc
	 * @param latitudeE6
	 * @param longitudeE6
	 * @param radiusMetres
	 * @return
	 */
	private boolean checkLocationConstraint(LocationConstraint lc,
			int latitudeE6, int longitudeE6, double radiusMetres) throws RequestException {
		if (lc.getType()!=null) {
			switch(lc.getType()) {
			case CIRCLE: {
				if (lc.getLatitudeE6()==null || lc.getLongitudeE6()==null)
					throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"locationContraint/CIRCLE requires latitudeE6 and longitudeE6");
				if (radiusMetres==0) {
					logger.info("GameInstance/Factory unlimited range - ok");
					return true; // unlimited range
				}
				LatLng l1 = new LatLng(lc.getLatitudeE6()/ONE_MILLION, lc.getLongitudeE6()/ONE_MILLION);
				LatLng l2 = new LatLng(latitudeE6/ONE_MILLION, longitudeE6/ONE_MILLION);
				// km to m
				double distanceMetres = l1.distance(l2)*ONE_THOUSAND;
				// if the game centre is in my range or i am in the game's range...
				// (i.e. if i could travel to the game, or i am already somewhere in range)
				// (just allowing any overlap is probably too optimistic, e.g. the
				//  nominal radius might include sea in order to be inclusive; requiring
				//  containment probably too restrictive, esp. for big playing areas which
				//  are likely to imply you only need to be in part of it)
				double queryRadius = (lc.getRadiusMetres()!=null) ? lc.getRadiusMetres() : 0;
				if (distanceMetres <= radiusMetres || distanceMetres <= queryRadius) {
					logger.info("GameInstance/Factory in range ("+distanceMetres+" vs "+radiusMetres+" and "+lc.getRadiusMetres()+")");
					return true;
				} else {
					logger.info("GameInstance/Factory out of range ("+distanceMetres+" vs "+radiusMetres+" and "+lc.getRadiusMetres()+")");
				}
				// failed
				return false;
			}							
			}
		}
		return true;
	}

	private List<GameClientTemplate> getGameClientTemplates(EntityManager em,
			GameQuery gq, String gameTemplateId) {
		return getGameClientTemplates(em, gq.getClientTitle(), gq.getClientType(), gameTemplateId, gq.getMajorVersion(), gq.getMinorVersion(), gq.getUpdateVersion());
	}
	static List<GameClientTemplate> getGameClientTemplates(EntityManager em,
			String clientTitle, GameClientType clientType,
			String gameTemplateId, Integer majorVersion, Integer minorVersion,
			Integer updateVersion) {

		// Check GameClientTemplate for clientType, clientTitle, locationSpecific and version
		Map<String,Object> qps = new HashMap<String,Object>();
		StringBuilder qb = new StringBuilder();
		qb.append("SELECT x FROM GameClientTemplate x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID);
		qps.put(GAME_TEMPLATE_ID, gameTemplateId);
		if (clientType!=null) {
			qb.append(" AND x."+CLIENT_TYPE+" = :"+CLIENT_TYPE);
			qps.put(CLIENT_TYPE, clientType);
		}
		if (clientTitle!=null) {
			qb.append(" AND x."+CLIENT_TITLE+" = :"+CLIENT_TITLE);
			qps.put(CLIENT_TITLE, clientTitle);
		}
		// can only check inequality on one value, so make that MAJOR_VERSION
		if (majorVersion!=null) {
			qb.append(" AND x."+MIN_MAJOR_VERSION+" <= :"+MAJOR_VERSION);
			qps.put(MAJOR_VERSION, majorVersion);
		}
		// post-check minor version & update version
		Query q = em.createQuery(qb.toString());
		for (String qp : qps.keySet()) {
			q.setParameter(qp, qps.get(qp));
		}
		List<GameClientTemplate> posgcts = (List<GameClientTemplate>)q.getResultList();
		// post-filter
		List<GameClientTemplate> gcts = new LinkedList<GameClientTemplate>();
		for (GameClientTemplate gct : posgcts) {
			if (majorVersion!=null && majorVersion==gct.getMinMajorVersion()) {
				// threshold major - check minor
				if (minorVersion!=null) {
					if (minorVersion < gct.getMinMajorVersion())
						continue; // no good
					if (minorVersion==gct.getMinMinorVersion()) {
						// threshold minor - check update
						if (updateVersion!=null) {
							if (updateVersion<gct.getMinUpdateVersion())
								continue; // no good
						}
					}
				}
			}
			// add gcts (if location ok)
			gcts.add(gct);
		}
		return gcts;
	}
	private static final String JOIN_PATH = "browser/JoinGameInstance/";
	private String makeJoinUrl(ServerConfiguration sc, GameInstance gi) {
		StringBuilder sb = new StringBuilder();
		if (sc.getBaseUrl()==null) {
			logger.warning("Server BaseURL not configured");
			sb.append(GetGameIndexServlet.DEFAULT_BASE_URL);
		}
		else {
			sb.append(sc.getBaseUrl());
			if (!sc.getBaseUrl().endsWith("/"))
				sb.append("/");			
		}
		sb.append(JOIN_PATH);
		sb.append(KeyFactory.keyToString(gi.getKey()));
		return sb.toString();
	}
}
