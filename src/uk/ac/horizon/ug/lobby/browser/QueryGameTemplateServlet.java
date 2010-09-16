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
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryLocationType;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryType;
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

	private GameTemplate getGameTemplate(HttpServletRequest req) throws RequestException {
		String id = HttpUtils.getIdFromPath(req);
		EntityManager em = EMF.get().createEntityManager();
		try {
			Key key = GameTemplate.idToKey(id);
			GameTemplate gt = em.find(GameTemplate.class, key);
	        if (gt==null)
	        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameTemplate "+id+" not found");
	        return gt;
		}
		finally {
			em.close();
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// parse request
		String line = null;
		GameQuery gq = null;
		GameTemplate gt = null;
		try {
			BufferedReader br = req.getReader();
			line = br.readLine();
			JSONObject json = new JSONObject(line);
			gq = JSONUtils.parseGameQuery(json);
			
			logger.info("GameQuery "+gq);
			gt = getGameTemplate(req);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		try {
			GameIndex gindex = handleGameQuery(gq, gt);
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
	}
	public GameIndex testHandleGameQuery(GameQuery gq, GameTemplate gt) throws RequestException, JSONException {
		return handleGameQuery(gq, gt);
	}
	public static final int DEFAULT_MAX_RESULTS = 30;
	static GameIndex handleGameQuery(GameQuery gq, GameTemplate gt) throws RequestException, JSONException {

		// get some of the general server info
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		GameIndex servergindex = sc.getGameIndex();
		GameIndex gindex = new GameIndex();
		gindex.setDocs(servergindex.getDocs());
		gindex.setGenerator(servergindex.getGenerator());
		gindex.setLastBuildDate(System.currentTimeMillis());
		gindex.setTtlMinutes(servergindex.getTtlMinutes());
		gindex.setVersion(servergindex.getVersion());
		
		// template-specific - describe template in top-level index
		gindex.setTitle(gt.getTitle());
		gindex.setDescription(gt.getDescription());
		gindex.setLanguage(gt.getLanguage());
		gindex.setImageUrl(gt.getImageUrl());
		gindex.setLink(gt.getLink());
		
		// for now actual instances but not factories will count against max
		int maxResults = DEFAULT_MAX_RESULTS;
		if (gq.getMaxResults()!=null)
			maxResults = gq.getMaxResults();
		if (maxResults<=0)
			return gindex;

		// matching combinations
		List<GameTemplateInfo> gtis = new LinkedList<GameTemplateInfo>();
		gindex.setItems(gtis);

		// Check GameClientTemplate for clientType, clientTitle, locationSpecific and version
		List<GameClientTemplate> posgcts = getGameClientTemplates(gq, gt.getId());
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
			// TODO more detail? alternative?
			gtis.add(getGameIndexMessage("Client not supported", "This game does not support you client", null));
			return gindex;
		}
		logger.info("Found "+gcts.size()+" possible client templates, of which "+noloc_gcts.size()+" location-independent");

		// ensure GameInstanceFactorys get a chance to create relevant GameInstances...
		EntityManager em = EMF.get().createEntityManager();
		try {
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
					if (gtis.size()>=maxResults)
						// no more instances...
						break;
				}
			}
		}
		finally {
			em.close();
		}
		em = EMF.get().createEntityManager();
		try {

			//==================================================================================
			// now check GameInstanceTemplates...
			// Check GameInstance for startTime, endTime, location (if location-specific) and nominalStatus (not CANCELLED)
			HashMap<String,Object> qps = new HashMap<String,Object>();
			StringBuilder qb = new StringBuilder();
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
			Query q = em.createQuery(qb.toString());
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
				if (gif.getServerCreateTimeOffsetMs()<0)
					minTime = minTime - gif.getServerCreateTimeOffsetMs(); // will make it bigger!
				if (tc!=null && tc.getMinTime()!=null && tc.getMinTime()>minTime)
					minTime = tc.getMinTime();
				// Can't factory start retrospectively 
				//if (tc!=null && tc.isIncludeStarted()) 
				// TODO long-running / variable length
				//minTime = minTime-gif.getDurationMs();
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
				if (firstStartTime<minTime) {
					logger.warning("FactoryUtils.getNextCronTime returned past startTime "+firstStartTime+" vs "+minTime);
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

				// GameInstanceFactory in useful itself
				GameTemplateInfo gti = new GameTemplateInfo();
				gti.setGameTemplate(gt);
				gti.setGameInstanceFactory(gif);
				// now in startTimeOptionsJson...
//				try {
//					gti.setGameTimeOptions(FactoryUtils.getGameTimeOptions(gif.getStartTimeCron(), firstStartTime));
//				} catch (CronExpressionException e) {
//					logger.warning("Generating GameTimeOptions: "+e);
//				}
				gti.setFirstStartTime(firstStartTime);
				if (gif.getType()==GameInstanceFactoryType.ON_DEMAND) {
					// link to GIF newInstance request handler
					gti.setNewInstanceUrl(makeNewInstanceUrl(sc, gif));
				}
				else {
					// note re-query, not join!
					gti.setQueryUrl(GetGameIndexServlet.makeQueryUrl(sc, gt));
				}
				if (locationOk)
					gti.setGameClientTemplates(gcts);
				else 
					gti.setGameClientTemplates(noloc_gcts);
				gtis.add(gti);
				
			}
			
			if (gtis.size()==0) {
				// TODO more detail, suggestions.
				gtis.add(getGameIndexMessage("No games found", "There were no games that matched your query", null));
			}
			return gindex;
		}
		finally {	
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
	private static boolean checkLocationConstraint(LocationConstraint lc,
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

	private static List<GameClientTemplate> getGameClientTemplates(GameQuery gq, String gameTemplateId) {
		return getGameClientTemplates(gq.getClientTitle(), gq.getClientType(), gameTemplateId, gq.getMajorVersion(), gq.getMinorVersion(), gq.getUpdateVersion());
	}
	static List<GameClientTemplate> getGameClientTemplates(String clientTitle, String clientType,
			String gameTemplateId, Integer majorVersion, Integer minorVersion,
			Integer updateVersion) {
		EntityManager em = EMF.get().createEntityManager();
		try {
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
		finally {
			em.close();
		}
	}
	/** make a 'GameTemplateInfo' just to convey a problem or alternative, not a game */
	private static GameTemplateInfo getGameIndexMessage(String message, String detail, String imageUrl) {
		GameTemplate gt = new GameTemplate();
		gt.setTitle(message);
		gt.setDescription(detail);
		if (imageUrl!=null)
			gt.setImageUrl(imageUrl);
		GameTemplateInfo gti = new GameTemplateInfo();
		gti.setGameTemplate(gt);
		return gti;
	}
	private static final String JOIN_PATH = "browser/JoinGameInstance/";
	static String makeJoinUrl(ServerConfiguration sc, GameInstance gi) {
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
	private static final String NEW_INSTANCE_PATH = "browser/NewGameInstance/";
	private static String makeNewInstanceUrl(ServerConfiguration sc, GameInstanceFactory gi) {
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
		sb.append(NEW_INSTANCE_PATH);
		sb.append(KeyFactory.keyToString(gi.getKey()));
		return sb.toString();
	}
}
