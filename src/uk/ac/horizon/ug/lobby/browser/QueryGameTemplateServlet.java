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
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameQuery;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.protocol.LocationConstraint;
import uk.ac.horizon.ug.lobby.protocol.TimeConstraint;
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

					if (lc.getType()!=null) {
						locationOk = false;
						switch(lc.getType()) {
						case CIRCLE: {
							if (lc.getLatitudeE6()==null || lc.getLongitudeE6()==null)
								throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"locationContraint/CIRCLE requires latitudeE6 and longitudeE6");
							if (gi.getRadiusMetres()==0) {
								locationOk = true;
								logger.info("GameInstance "+gi.getTitle()+" unlimited range - ok");
								break; // unlimited range
							}
							LatLng l1 = new LatLng(lc.getLatitudeE6()/ONE_MILLION, lc.getLongitudeE6()/ONE_MILLION);
							LatLng l2 = new LatLng(gi.getLatitudeE6()/ONE_MILLION, gi.getLongitudeE6()/ONE_MILLION);
							// km to m
							double distanceMetres = l1.distance(l2)*ONE_THOUSAND;
							// if the game centre is in my range or i am in the game's range...
							// (i.e. if i could travel to the game, or i am already somewhere in range)
							// (just allowing any overlap is probably too optimistic, e.g. the
							//  nominal radius might include sea in order to be inclusive; requiring
							//  containment probably too restrictive, esp. for big playing areas which
							//  are likely to imply you only need to be in part of it)
							double queryRadius = (lc.getRadiusMetres()!=null) ? lc.getRadiusMetres() : 0;
							if (distanceMetres <= gi.getRadiusMetres() || distanceMetres <= queryRadius) {
								locationOk = true;
								logger.info("GameInstance "+gi.getTitle()+" in range ("+distanceMetres+" vs "+gi.getRadiusMetres()+" and "+lc.getRadiusMetres()+")");
								break;
							} else {
								locationOk = false;
								logger.info("GameInstance "+gi.getTitle()+" out of range ("+distanceMetres+" vs "+gi.getRadiusMetres()+" and "+lc.getRadiusMetres()+")");
							}
							// failed
							break;
						}							
						}
					}
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
			em.close();
		}
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
