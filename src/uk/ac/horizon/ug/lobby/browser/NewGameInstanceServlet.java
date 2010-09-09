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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryType;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceSlot;
import uk.ac.horizon.ug.lobby.model.GameInstanceSlotStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequest;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequestType;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponse;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponseStatus;
import uk.ac.horizon.ug.lobby.protocol.GameQuery;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.protocol.LocationConstraint;
import uk.ac.horizon.ug.lobby.protocol.TimeConstraint;
import uk.ac.horizon.ug.lobby.server.CronExpressionException;
import uk.ac.horizon.ug.lobby.server.FactoryTasks;
import uk.ac.horizon.ug.lobby.server.FactoryUtils;
import uk.ac.horizon.ug.lobby.server.ServerProtocol;
import uk.ac.horizon.ug.lobby.user.UserGameTemplateServlet;
import uk.me.jstott.jcoord.LatLng;

/** 
 * Handle request to create new GameInstance from Factory.
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class NewGameInstanceServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(NewGameInstanceServlet.class.getName());

	private GameInstanceFactory getGameInstanceFactory(HttpServletRequest req, EntityManager em) throws RequestException {
		String id = HttpUtils.getIdFromPath(req);
		
		Key key = KeyFactory.stringToKey(id);
		GameInstanceFactory gt = em.find(GameInstanceFactory.class, key);
        if (gt==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameInstanceFactory "+id+" not found");
        
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
			GameJoinRequest gjreq = JSONUtils.parseGameJoinRequest(json);
			// second line is digital signature (if given)
			String auth = br.readLine();
			
			logger.info("GameJoinRequest "+gjreq);
			// check type supported...
			if (gjreq.getType()!=GameJoinRequestType.NEW_INSTANCE) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request type must be NEW_INSTANCE ("+gjreq.getType()+")");
				return;
			}
			GameInstanceFactory gif = getGameInstanceFactory(req, em);
			
			GameJoinResponse gjresp = new GameJoinResponse();
			gjresp.setTime(System.currentTimeMillis());
			
			// validate 
			gjresp.setType(gjreq.getType());
			
			// authenticate client
			// TODO: check isCreateForAnonymousClients later
			JoinUtils.JoinAuthInfo jai = JoinUtils.authenticate(gjreq, gjresp, gif.isAllowAnonymousClients(), resp, line, auth);
			if (jai==null)
				// dealt with
				return;
			GameClient gc = jai.gc;
			Account account = jai.account;
			boolean anonymous = jai.anonymous;
			
			gjresp.setClientId(gc.getId());

			// we know it is a NEW_INSTANCE request...
			
			if (gif.getType()!=GameInstanceFactoryType.ON_DEMAND) {
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_SCHEDULED_ONLY, "This game does not support on-request instances");
				return;
			}
			if (gif.getStatus()!=GameInstanceFactoryStatus.ACTIVE) {
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_NOT_PERMITTED, "This game factory is not active");
				return;
			}
			if (gif.getStartTimeOptionsJson()==null) {
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_INVALID, "This game factory has no available start time(s)");
				return;				
			}
			if (gjreq.getNewInstanceStartTime()==null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "NEW_INSTANCE request must have newInstanceStartTime");
				return;
			}
			long newInstanceStartTime = gjreq.getNewInstanceStartTime();
			if (newInstanceStartTime<gif.getMinTime() || newInstanceStartTime>gif.getMaxTime()) {
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_INVALID, "NewInstanceStartTime out of range for this game factory");
				return;						
			}
			// lee-way in timing expressed (1 minute?!)
			long START_TIME_RANGE_MS = 60000;
			// allow start-up time on server
			long earliest = System.currentTimeMillis();
			if (gif.getServerCreateTimeOffsetMs()<0)
				earliest = earliest - gif.getServerCreateTimeOffsetMs();
			if (newInstanceStartTime+START_TIME_RANGE_MS < earliest) {
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_TOO_SOON, "NewInstanceStartTime too soon (in "+(newInstanceStartTime-System.currentTimeMillis())+"ms)");
				return;										
			} else if (newInstanceStartTime < earliest)
				// leave enough time...
				newInstanceStartTime = earliest;
			// round up to allowed times
			try {
				TreeSet timeOptions[] = FactoryUtils.parseTimeOptionsJson(gif.getStartTimeOptionsJson());
				newInstanceStartTime = FactoryUtils.getNextCronTime(gif.getStartTimeCron(), timeOptions, newInstanceStartTime, gif.getMaxTime());
			} catch (CronExpressionException e) {
				logger.warning("Checking nextStartTime: "+e);
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_INVALID, "Problem with checking nextStartTime");
				return;				
			}
			if (newInstanceStartTime!=0 && newInstanceStartTime>gjreq.getNewInstanceStartTime()+START_TIME_RANGE_MS) {
				if (gjreq.getNewInstanceStartTime() < earliest)
					JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_TOO_SOON, "NewInstanceStartTime too soon (in "+(newInstanceStartTime-System.currentTimeMillis())+"ms)");
				else
					// rounded up 'too' far to find a valid start time
					JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_INVALID, "Proposed startTime is not (close to) a valid startTime");
				return;				
			}
			if (newInstanceStartTime==0 || newInstanceStartTime>gif.getMaxTime()) {
				JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_START_TIME_INVALID, "NewInstanceStartTime out of range for this game factory once correctly for allowed starts ("+newInstanceStartTime+")");
				return;									
			}

			// does this instance exist already? 
			// TODO: if number of concurrent instances is constrained then HIDDEN and FULL instances should also be considered!
			Query q = em.createQuery("SELECT x FROM GameInstance x WHERE x."+GAME_INSTANCE_FACTORY_KEY+" = :"+GAME_INSTANCE_FACTORY_KEY+" AND x."+VISIBILITY+" = '"+GameTemplateVisibility.PUBLIC.toString()+"' AND x."+FULL+" = FALSE");
			q.setMaxResults(1);
			List<GameInstance> gis = (List<GameInstance>)q.getResultList();
			GameInstance gi = null;
			if (gis.size()>0) {
				// essentially we now treat this as a JOIN ?!
				gi = gis.get(0);
				et.rollback();
			}
			else {
				// doesn't exist - perhaps we'll make it
				et.rollback();
				et.begin();
				
				// is it far enough in advance?
				
				
				// create new instance?!
				if (anonymous && !gif.isCreateForAnonymousClient()) {
					JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game factory will not create for anonymous players");
					return;
				}
				// update quota... (doesn't do anything else for non-scheduled factories)
				FactoryTasks.checkGameInstanceFactory(sc, gif);
				// check quota...
				et.rollback();
				et.begin();
				GameInstanceFactory ngif = em.find(GameInstanceFactory.class, gif.getKey());
				int tokenCache = ngif.getNewInstanceTokens();
				if (tokenCache<=0) {
					JoinUtils.sendError(resp, gjresp, GameJoinResponseStatus.ERROR_SYSTEM_QUOTA_EXCEEDED, "This game factory cannot create any more instances at present");
					return;					
				}
				et.rollback();
				// create!
				gi = FactoryTasks.createGameInstanceFactoryInstance(ngif, newInstanceStartTime, account, req.getRemoteAddr(), null);
				
			}
			
			//et.commit();
			// follow-on info
			gjresp.setJoinUrl(QueryGameTemplateServlet.makeJoinUrl(sc, gi));

			// now attempt a RESERVE on the identified game instance
			gjreq.setType(GameJoinRequestType.RESERVE);

			et.begin();
			JoinGameInstanceServlet.handleJoinRequestInternal(req, resp, sc, em, et, gjreq, gjresp, jai, gi);
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		finally {	
			if(et.isActive())
				et.rollback();
			em.close();
		}
	}
}
