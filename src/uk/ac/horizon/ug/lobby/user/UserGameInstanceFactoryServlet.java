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
package uk.ac.horizon.ug.lobby.user;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService; 
import com.google.appengine.api.users.UserServiceFactory; 
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.HttpUtils;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.server.CronExpressionException;
import uk.ac.horizon.ug.lobby.server.FactoryUtils;

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class UserGameInstanceFactoryServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(UserGameInstanceFactoryServlet.class.getName());
	
	static class GameInstanceFactoryInfo {
		Account account;
		GameInstanceFactory gameInstanceFactory;
		GameTemplate gameTemplate;
		GameServer gameServer;
	}
	
	private GameInstanceFactoryInfo getGameInstanceFactoryInfo(HttpServletRequest req, EntityManager em) throws RequestException {
		Account account = AccountUtils.getAccount(req);
		return getGameInstanceFactoryInfo(req, em, account);
	}
	private GameInstanceFactoryInfo getGameInstanceFactoryInfo(HttpServletRequest req, EntityManager em, Account account) throws RequestException {
		GameInstanceFactoryInfo gii = new GameInstanceFactoryInfo();
		gii.account = account;
		
		String id = HttpUtils.getIdFromPath(req);
		
		Key key = KeyFactory.stringToKey(id);
		GameInstanceFactory gs = em.find(GameInstanceFactory.class, key);
        if (gs==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameInstanceFactory "+id+" not found");
        gii.gameInstanceFactory = gs;
        
        key = GameTemplate.idToKey(gs.getGameTemplateId());
        GameTemplate gt = em.find(GameTemplate.class, key);
        if (gt==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameTemplate "+gs.getGameTemplateId()+" for GameInstance "+id+" not found");
        
        if (!account.getKey().equals(gt.getOwnerId())) 
        	throw new RequestException(HttpServletResponse.SC_FORBIDDEN, "User is not owner for instance factory "+id);
        
        if (gs.getGameServerId()!=null) {
        	gii.gameServer = em.find(GameServer.class, gs.getGameServerId());
        	if (gii.gameServer==null)
        		logger.warning("GameServer "+key+" not found for GameInstanceFactory "+id);
        }
        
        return gii;
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameInstanceFactoryInfo gii = getGameInstanceFactoryInfo(req, em);
			JSONUtils.sendGameInstanceFactory(resp, gii.gameInstanceFactory, gii.gameTemplate, gii.gameServer);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
		}
		finally {
			em.close();
		}
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Account account = null;
		try {
			account = AccountUtils.getAccount(req);
		} catch (RequestException e1) {
			resp.sendError(e1.getErrorCode(), e1.getMessage());
			return;
		}
		EntityManager em = EMF.get().createEntityManager();
		GameInstanceFactoryInfo gii = null;
		try {
			gii = getGameInstanceFactoryInfo(req, em, account);	
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			GameInstanceFactory ngi = JSONUtils.parseGameInstanceFactory(json);
			
			if (ngi.getKey()!=null && !KeyFactory.keyToString(gii.gameInstanceFactory.getKey()).equals(KeyFactory.keyToString(ngi.getKey())))
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameInstanceFactory key in URL ("+gii.gameInstanceFactory.getKey()+") does not match key in data ("+ngi.getKey()+")");
			if (ngi.getGameTemplateId()!=null && !ngi.getGameTemplateId().equals(gii.gameInstanceFactory.getGameTemplateId()))
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameInstanceFactory cannot change gameTemplateId");
			
			if (ngi.getGameServerId()!=null) {
				GameServer gs = em.find(GameServer.class, ngi.getGameServerId());
				if (gs==null) 
					throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameInstanceFactory proposed GameServer "+ngi.getGameServerId()+" not found");
				if (!KeyFactory.keyToString(gs.getOwnerId()).equals(KeyFactory.keyToString(account.getKey()))) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Update GameInstanceFactory GameServer '"+gii.gameInstanceFactory.getGameServerId()+"' not owned by "+account.getNickname());
					return;				
				}
				gii.gameServer = gs;
			}
			
			logger.info("Update GameInstanceFactory "+gii.gameInstanceFactory+" -> "+ngi);

			// restricted update...
			
			// note check of json, not ngi (which has default value(s))
			if (json.has(ALLOW_ANONYMOUS_CLIENTS))
				gii.gameInstanceFactory.setAllowAnonymousClients(ngi.isAllowAnonymousClients());
			// note check of json, not ngi (which has default value(s))
			if (json.has(ALLOW_PRIVATE_INSTANCES))
				gii.gameInstanceFactory.setAllowPrivateInstances(ngi.isAllowPrivateInstances());
			// note check of json, not ngi (which has default value(s))
			if (json.has(CREATE_FOR_ANONYMOUS_CLIENT))
				gii.gameInstanceFactory.setCreateForAnonymousClient(ngi.isCreateForAnonymousClient());
			if (ngi.getType()!=null) 
				gii.gameInstanceFactory.setType(ngi.getType());
			// note check of json, not ngi (which has default value(s))
			if (json.has(DURATION_MS))
				gii.gameInstanceFactory.setDurationMs(ngi.getDurationMs());
			// note check of json, not ngi (which has default value(s))
			if (ngi.getGameServerId()!=null)
				gii.gameInstanceFactory.setGameServerId(ngi.getGameServerId());
			if (ngi.getInstanceTitle()!=null)
				gii.gameInstanceFactory.setInstanceTitle(ngi.getInstanceTitle());
			if (ngi.getInstanceVisibility()!=null)
				gii.gameInstanceFactory.setInstanceVisibility(ngi.getInstanceVisibility());
			// note check of json, not ngi (which has default value(s))
			if (json.has(INSTANCE_CREATE_TIME_WINDOW_MS))
				gii.gameInstanceFactory.setInstanceCreateTimeWindowMs(ngi.getInstanceCreateTimeWindowMs());
			// not lastInstanceCheckTime
			// not lastInstanceStartTime
			if (ngi.getLocationName()!=null)
				gii.gameInstanceFactory.setLocationName(ngi.getLocationName());
			if (ngi.getLocationType()!=null)
				gii.gameInstanceFactory.setLocationType(ngi.getLocationType());
			// note check of json, not ngi (which has default value(s))
			if (json.has(LATITUDE_E6))
				gii.gameInstanceFactory.setLatitudeE6(ngi.getLatitudeE6());
			// note check of json, not ngi (which has default value(s))
			if (json.has(LONGITUDE_E6))
				gii.gameInstanceFactory.setLongitudeE6(ngi.getLongitudeE6());
			// not newInstanceTokens
			// note check of json, not ngi (which has default value(s))
			if (json.has(NEW_INSTANCE_TOKENS_MAX))
				gii.gameInstanceFactory.setNewInstanceTokensMax(ngi.getNewInstanceTokensMax());
			// note check of json, not ngi (which has default value(s))
			if (json.has(NEW_INSTANCE_TOKENS_PER_HOUR))
				gii.gameInstanceFactory.setNewInstanceTokensPerHour(ngi.getNewInstanceTokensPerHour());
			// note check of json, not ngi (which has default value(s))
			if (json.has(MAX_NUM_SLOTS))
				gii.gameInstanceFactory.setMaxNumSlots(ngi.getMaxNumSlots());
			// note check of json, not ngi (which has default value(s))
			if (json.has(MAX_TIME))
				gii.gameInstanceFactory.setMaxTime(ngi.getMaxTime());
			// note check of json, not ngi (which has default value(s))
			if (json.has(MIN_TIME))
				gii.gameInstanceFactory.setMinTime(ngi.getMinTime());
			// note check of json, not ngi (which has default value(s))
			if (json.has(RADIUS_METRES))
				gii.gameInstanceFactory.setRadiusMetres(ngi.getRadiusMetres());
			if (ngi.getServerConfigJson()!=null)
				gii.gameInstanceFactory.setServerConfigJson(ngi.getServerConfigJson());
			// note check of json, not ngi (which has default value(s))
			if (json.has(SERVER_CREATE_TIME_OFFSET_MS))
				gii.gameInstanceFactory.setServerCreateTimeOffsetMs(ngi.getServerCreateTimeOffsetMs());
			// note check of json, not ngi (which has default value(s))
			if (json.has(SERVER_END_TIME_OFFSET_MS))
				gii.gameInstanceFactory.setServerEndTimeOffsetMs(ngi.getServerEndTimeOffsetMs());
			// note check of json, not ngi (which has default value(s))
			if (json.has(SERVER_ENDING_TIME_OFFSET_MS))
				gii.gameInstanceFactory.setServerEndingTimeOffsetMs(ngi.getServerEndingTimeOffsetMs());
			// note check of json, not ngi (which has default value(s))
			if (json.has(SERVER_START_TIME_OFFSET_MS))
				gii.gameInstanceFactory.setServerStartTimeOffsetMs(ngi.getServerStartTimeOffsetMs());
			if (ngi.getStartTimeCron()!=null)
				gii.gameInstanceFactory.setStartTimeCron(ngi.getStartTimeCron());
			// not startTimeOptionsCron
			if (ngi.getStatus()!=null)
				// TODO act on status?!
				gii.gameInstanceFactory.setStatus(ngi.getStatus());
			if (ngi.getTitle()!=null)
				gii.gameInstanceFactory.setTitle(ngi.getTitle());
			if (ngi.getVisibility()!=null)
				gii.gameInstanceFactory.setVisibility(ngi.getVisibility());

			// cache state
			// cache state
			try {
				if (gii.gameInstanceFactory.getStartTimeCron()==null)
					gii.gameInstanceFactory.setStartTimeOptionsJson(null);
				else
					gii.gameInstanceFactory.setStartTimeOptionsJson(FactoryUtils.getTimeOptionsJson(gii.gameInstanceFactory.getStartTimeCron()));
			}
			catch (CronExpressionException e) {
				// TODO log error
			}
			// change of possible instances - force recheck
			if (json.has(START_TIME_CRON) || json.has(MIN_TIME) || json.has(MAX_TIME)) 
				gii.gameInstanceFactory.setLastInstanceStartTime(0);
			// ?!
			
			em.merge(gii.gameInstanceFactory);
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
		JSONUtils.sendGameInstanceFactory(resp, gii.gameInstanceFactory, gii.gameTemplate, gii.gameServer);
	}
}
