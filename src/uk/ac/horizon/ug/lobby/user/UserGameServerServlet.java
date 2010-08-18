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
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
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
public class UserGameServerServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(UserGameServerServlet.class.getName());
	
	private String getGameServerId(HttpServletRequest req) throws RequestException {
		String id = req.getPathInfo();
		if (id!=null && id.startsWith("/"))
			id = id.substring(1);
		if (id==null || id.length()==0)
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "No GameServer specified in path");
		return id;
	}

	private GameServer getGameServer(HttpServletRequest req, EntityManager em) throws RequestException {
		Account account = AccountUtils.getAccount(req);
		return getGameServer(req, em, account);
	}
	private GameServer getGameServer(HttpServletRequest req, EntityManager em, Account account) throws RequestException {
		String id = getGameServerId(req);
		
		Key key = KeyFactory.stringToKey(id);
		GameServer gs = em.find(GameServer.class, key);
        if (gs==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameServer "+id+" not found");
        
        if (!account.getKey().equals(gs.getOwnerId())) 
        	throw new RequestException(HttpServletResponse.SC_FORBIDDEN, "User is not owner for server "+id);
        
        return gs;
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameServer gs = getGameServer(req, em);
			JSONUtils.sendGameServer(resp, gs);
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
		GameServer gs = null;
		try {
			gs = getGameServer(req, em, account);	
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			GameServer ngs = JSONUtils.parseGameServer(json);
			
			if (ngs.getKey()!=null && !KeyFactory.keyToString(gs.getKey()).equals(KeyFactory.keyToString(ngs.getKey())))
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameServer key in URL ("+gs.getKey()+") does not match key in data ("+ngs.getKey()+")");
			
			logger.info("Update GameServer "+gs+" -> "+ngs);
			// fix up
			if (ngs.getTargetStatus()!=null)
				gs.setTargetStatus(ngs.getTargetStatus());
			if (ngs.getBaseUrl()!=null)
				gs.setBaseUrl(ngs.getBaseUrl());
			if (ngs.getGameTemplateId()!=null)
				gs.setGameTemplateId(ngs.getGameTemplateId());
			if (ngs.getLobbySharedSecret()!=null)
				gs.setLobbySharedSecret(ngs.getLobbySharedSecret());
			if (ngs.getType()!=null)
				gs.setType(ngs.getType());
			if (ngs.getTitle()!=null)
				gs.setTitle(ngs.getTitle());

			em.merge(gs);
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
		JSONUtils.sendGameServer(resp, gs);
	}
}
