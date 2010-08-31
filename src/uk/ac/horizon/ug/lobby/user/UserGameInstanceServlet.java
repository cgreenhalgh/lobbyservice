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
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class UserGameInstanceServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(UserGameInstanceServlet.class.getName());
	
	static class GameInstanceInfo {
		Account account;
		GameInstance gameInstance;
		GameTemplate gameTemplate;
		GameServer gameServer;
	}
	
	private GameInstanceInfo getGameInstanceInfo(HttpServletRequest req, EntityManager em) throws RequestException {
		Account account = AccountUtils.getAccount(req);
		return getGameInstanceInfo(req, em, account);
	}
	private GameInstanceInfo getGameInstanceInfo(HttpServletRequest req, EntityManager em, Account account) throws RequestException {
		GameInstanceInfo gii = new GameInstanceInfo();
		gii.account = account;
		
		String id = HttpUtils.getIdFromPath(req);
		
		Key key = KeyFactory.stringToKey(id);
		GameInstance gs = em.find(GameInstance.class, key);
        if (gs==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameInstance "+id+" not found");
        gii.gameInstance = gs;
        
        key = GameTemplate.idToKey(gs.getGameTemplateId());
        GameTemplate gt = em.find(GameTemplate.class, key);
        if (gt==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameTemplate "+gs.getGameTemplateId()+" for GameInstance "+id+" not found");
        
        if (!account.getKey().equals(gt.getOwnerId())) 
        	throw new RequestException(HttpServletResponse.SC_FORBIDDEN, "User is not owner for instance "+id);
        
        if (gs.getGameServerId()!=null) {
        	gii.gameServer = em.find(GameServer.class, gs.getGameServerId());
        	if (gii.gameServer==null)
        		logger.warning("GameServer "+key+" not found for GameInstance "+id);
        }
        
        return gii;
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameInstanceInfo gii = getGameInstanceInfo(req, em);
			JSONUtils.sendGameInstance(resp, gii.gameInstance, gii.gameTemplate, gii.gameServer);
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
		GameInstanceInfo gii = null;
		try {
			gii = getGameInstanceInfo(req, em, account);	
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			GameInstance ngi = JSONUtils.parseGameInstance(json);
			
			if (ngi.getKey()!=null && !KeyFactory.keyToString(gii.gameInstance.getKey()).equals(KeyFactory.keyToString(ngi.getKey())))
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameInstance key in URL ("+gii.gameInstance.getKey()+") does not match key in data ("+ngi.getKey()+")");
			if (ngi.getGameTemplateId()!=null && !ngi.getGameTemplateId().equals(gii.gameInstance.getGameTemplateId()))
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameInstance cannot change gameTemplateId");
			
			if (ngi.getGameServerId()!=null) {
				GameServer gs = em.find(GameServer.class, ngi.getGameServerId());
				if (gs==null) 
					throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameInstance proposed GameServer "+ngi.getGameServerId()+" not found");
				if (!KeyFactory.keyToString(gs.getOwnerId()).equals(KeyFactory.keyToString(account.getKey()))) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Update GameInstance GameServer '"+gii.gameInstance.getGameServerId()+"' not owned by "+account.getNickname());
					return;				
				}
				gii.gameServer = gs;
			}
			
			logger.info("Update GameInstance "+gii.gameInstance+" -> "+ngi);
			// fix up
			if (ngi.getNominalStatus()!=null)
				gii.gameInstance.setNominalStatus(ngi.getNominalStatus());
			if (ngi.getBaseUrl()!=null)
				gii.gameInstance.setBaseUrl(ngi.getBaseUrl());
			// TODO can't set to 0 ?!
			if (ngi.getEndTime()!=0)
				gii.gameInstance.setEndTime(ngi.getEndTime());
			if (ngi.getStartTime()!=0)
				gii.gameInstance.setStartTime(ngi.getStartTime());
			if (ngi.getRadiusMetres()!=0)
				gii.gameInstance.setRadiusMetres(ngi.getRadiusMetres());
			if (ngi.getGameServerId()!=null)
				gii.gameInstance.setGameServerId(ngi.getGameServerId());
			if (ngi.getLocationName()!=null)
				gii.gameInstance.setLocationName(ngi.getLocationName());
			if (ngi.getLatitudeE6()!=0)
				gii.gameInstance.setLatitudeE6(ngi.getLatitudeE6());
			if (ngi.getLongitudeE6()!=0)
				gii.gameInstance.setLongitudeE6(ngi.getLongitudeE6());
			// note check of json, not ngi (which has default value(s))
			if (json.has(MAX_NUM_SLOTS))
				gii.gameInstance.setMaxNumSlots(ngi.getMaxNumSlots());
			// note check of json, not ngi (which has default value(s))
			if (json.has(ALLOW_ANONYMOUS_CLIENTS))
				gii.gameInstance.setAllowAnonymousClients(ngi.isAllowAnonymousClients());
			if (ngi.getVisibility()!=null)
				gii.gameInstance.setVisibility(ngi.getVisibility());

			// cache state
			gii.gameInstance.setFull(gii.gameInstance.getNumSlotsAllocated()>=gii.gameInstance.getMaxNumSlots());
			
			em.merge(gii.gameInstance);
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
		JSONUtils.sendGameInstance(resp, gii.gameInstance, gii.gameTemplate, gii.gameServer);
	}
}
