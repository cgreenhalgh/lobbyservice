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
import uk.ac.horizon.ug.lobby.browser.JoinUtils.JoinAuthInfo;
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

	private GameInstanceFactory getGameInstanceFactory(HttpServletRequest req) throws RequestException {
		String id = HttpUtils.getIdFromPath(req);
		EntityManager em = EMF.get().createEntityManager();
		try {
			Key key = KeyFactory.stringToKey(id);
			GameInstanceFactory gt = em.find(GameInstanceFactory.class, key);
			if (gt==null)
				throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameInstanceFactory "+id+" not found");

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
		String auth = null;
		GameJoinRequest gjreq = null;
		try {
			BufferedReader br = req.getReader();
			line = br.readLine();
			JSONObject json = new JSONObject(line);
			gjreq = JSONUtils.parseGameJoinRequest(json);
			// second line is digital signature (if given)
			auth = br.readLine();
			
			logger.info("GameJoinRequest "+gjreq);
			// check type supported...
			if (gjreq.getType()!=GameJoinRequestType.NEW_INSTANCE) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request type must be NEW_INSTANCE ("+gjreq.getType()+")");
				return;
			}
		}
		catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;			
		}
		//ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		try {
			GameInstanceFactory gif = getGameInstanceFactory(req);
			
			GameJoinResponse gjresp = new GameJoinResponse();
			gjresp.setTime(System.currentTimeMillis());
			
			// validate 
			gjresp.setType(gjreq.getType());
			
			// authenticate client
			JoinUtils.ClientInfo clientInfo = new JoinUtils.ClientInfo(gjreq.getClientType(), gjreq.getMajorVersion(), gjreq.getMinorVersion(), gjreq.getUpdateVersion());
			JoinUtils.JoinAuthInfo jai = JoinUtils.authenticate(gjreq.getClientId(), gjreq.getDeviceId(), clientInfo, gif.isAllowAnonymousClients(), line, auth);
			GameClient gc = jai.gc;
			Account account = jai.account;
			boolean anonymous = jai.anonymous;
			
			gjresp.setClientId(gc.getId());

			handleNewInstanceRequest(gjreq, gjresp, jai, gif, req.getRemoteAddr());
			
			JSONUtils.sendGameJoinResponse(resp, gjresp);
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JoinException e) {
			GameJoinResponse gjresp = new GameJoinResponse();
			gjresp.setTime(System.currentTimeMillis());
			gjresp.setType(gjreq.getType());
			gjresp.setStatus(e.getStatus());
			gjresp.setMessage(e.getMessage());
			logger.warning(e.toString());
			JSONUtils.sendGameJoinResponse(resp, gjresp);
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
	}
	private void handleNewInstanceRequest(GameJoinRequest gjreq,
			GameJoinResponse gjresp, JoinAuthInfo jai, GameInstanceFactory gif, String clientAddr) throws JoinException, RequestException, JSONException, IOException {
		GameClient gc = jai.gc;
		Account account = jai.account;
		boolean anonymous = jai.anonymous;

		// we know it is a NEW_INSTANCE request...

		if (gif.getType()!=GameInstanceFactoryType.ON_DEMAND) {
			throw new JoinException(GameJoinResponseStatus.ERROR_SCHEDULED_ONLY, "This game does not support on-request instances");
		}
		if (gif.getStatus()!=GameInstanceFactoryStatus.ACTIVE) {
			throw new JoinException(GameJoinResponseStatus.ERROR_NOT_PERMITTED, "This game factory is not active");
		}
		if (gif.getStartTimeOptionsJson()==null) {
			throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_INVALID, "This game factory has no available start time(s)");
		}
		if (gjreq.getNewInstanceStartTime()==null) {
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "NEW_INSTANCE request must have newInstanceStartTime");
		}
		long newInstanceStartTime = gjreq.getNewInstanceStartTime();
		if (newInstanceStartTime<gif.getMinTime() || newInstanceStartTime>gif.getMaxTime()) {
			throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_INVALID, "NewInstanceStartTime out of range for this game factory");
		}
		// lee-way in timing expressed (1 minute?!)
		long START_TIME_RANGE_MS = 60000;
		// allow start-up time on server
		long earliest = System.currentTimeMillis();
		if (gif.getServerCreateTimeOffsetMs()<0)
			earliest = earliest - gif.getServerCreateTimeOffsetMs();
		if (newInstanceStartTime+START_TIME_RANGE_MS < earliest) {
			throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_TOO_SOON, "NewInstanceStartTime too soon (in "+(newInstanceStartTime-System.currentTimeMillis())+"ms)");
		} else if (newInstanceStartTime < earliest)
			// leave enough time...
			newInstanceStartTime = earliest;
		// round up to allowed times
		try {
			TreeSet timeOptions[] = FactoryUtils.parseTimeOptionsJson(gif.getStartTimeOptionsJson());
			newInstanceStartTime = FactoryUtils.getNextCronTime(gif.getStartTimeCron(), timeOptions, newInstanceStartTime, gif.getMaxTime());
		} catch (CronExpressionException e) {
			logger.warning("Checking nextStartTime: "+e);
			throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_INVALID, "Problem with checking nextStartTime");
		}
		if (newInstanceStartTime!=0 && newInstanceStartTime>gjreq.getNewInstanceStartTime()+START_TIME_RANGE_MS) {
			if (gjreq.getNewInstanceStartTime() < earliest)
				throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_TOO_SOON, "NewInstanceStartTime too soon (in "+(newInstanceStartTime-System.currentTimeMillis())+"ms)");
			else
				// rounded up 'too' far to find a valid start time
				throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_INVALID, "Proposed startTime is not (close to) a valid startTime");
		}
		if (newInstanceStartTime==0 || newInstanceStartTime>gif.getMaxTime()) {
			throw new JoinException(GameJoinResponseStatus.ERROR_START_TIME_INVALID, "NewInstanceStartTime out of range for this game factory once correctly for allowed starts ("+newInstanceStartTime+")");
		}
		GameInstance gi = null;
		
		if (gjreq.getNewInstanceVisibility()!=null && gjreq.getNewInstanceVisibility()!=gif.getInstanceVisibility() && gjreq.getNewInstanceVisibility()==GameTemplateVisibility.HIDDEN)
			// can't 
			throw new JoinException(GameJoinResponseStatus.ERROR_NOT_PERMITTED, "Cannot create a hidden (private) instance of this game");

		if (gjreq.getNewInstanceVisibility()!=GameTemplateVisibility.HIDDEN) {
			// check if it already exists...
			EntityManager em = EMF.get().createEntityManager();
			try {
				// does this instance exist already? 
				// TODO: if number of concurrent instances is constrained then HIDDEN and FULL instances should also be considered!
				Query q = em.createQuery("SELECT x FROM GameInstance x WHERE x."+GAME_INSTANCE_FACTORY_KEY+" = :"+GAME_INSTANCE_FACTORY_KEY+" AND x."+VISIBILITY+" = '"+GameTemplateVisibility.PUBLIC.toString()+"' AND x."+FULL+" = FALSE");
				q.setMaxResults(1);
				List<GameInstance> gis = (List<GameInstance>)q.getResultList();
				if (gis.size()>0) {
					// essentially we now treat this as a JOIN ?!
					gi = gis.get(0);
					//et.rollback();
				}
			}
			finally {
				em.close();
			}
		}
		// needed in a minute
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();

		if (gi==null) {
			// doesn't exist - perhaps we'll make it

			// is it far enough in advance?
			// TODO

			// create new instance?!
			if (anonymous && !gif.isCreateForAnonymousClient()) {
				throw new JoinException(GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game factory will not create for anonymous players");
			}
			// update quota... (doesn't do anything else for non-scheduled factories)
			FactoryTasks.checkGameInstanceFactory(sc, gif);
			// check quota...
			EntityManager em = EMF.get().createEntityManager();
			EntityTransaction et = em.getTransaction();
			// transaction
			et.begin();
			GameInstanceFactory ngif = null;
			try {
				ngif = em.find(GameInstanceFactory.class, gif.getKey());
				int tokenCache = ngif.getNewInstanceTokens();
				if (tokenCache<=0) {
					throw new JoinException(GameJoinResponseStatus.ERROR_SYSTEM_QUOTA_EXCEEDED, "This game factory cannot create any more instances at present");
				}
			}
			finally {
				et.rollback();
				em.close();
			}
			// create! (over-ride visibility)
			gi = FactoryTasks.createGameInstanceFactoryInstance(ngif, gjreq.getNewInstanceVisibility(), newInstanceStartTime, account, clientAddr, null);
		}

		// new or existing?!
		// follow-on info
		gjresp.setJoinUrl(QueryGameTemplateServlet.makeJoinUrl(sc, gi));

		// now attempt a RESERVE on the identified game instance
		gjreq.setType(GameJoinRequestType.RESERVE);

		JoinGameInstanceServlet.handleJoinRequestInternal(gjreq, gjresp, jai, gi);
	}
}
