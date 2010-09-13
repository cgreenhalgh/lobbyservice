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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.KeyFactory;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
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
public class AddGameInstanceServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(AddGameInstanceServlet.class.getName());
	
	public static class GameInstanceInfo {
		public GameInstance gi;
		public GameTemplate gt;
		public GameServer gs;
		public GameInstanceInfo() {}
		/**
		 * @param gi
		 * @param gt
		 * @param gs
		 */
		public GameInstanceInfo(GameInstance gi, GameTemplate gt, GameServer gs) {
			super();
			this.gi = gi;
			this.gt = gt;
			this.gs = gs;
		}
		
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
        GameInstance gi = null;
		try {
			BufferedReader r = req.getReader();
			String line = r.readLine();
			// why does this seem to read {} ??
			//JSONObject json = new JSONObject(req.getReader());
			JSONObject json = new JSONObject(line);
			gi = JSONUtils.parseGameInstance(json);
		}
		catch (JSONException je) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, je.toString());
			return;
		}
        
		try {
			Account account = AccountUtils.getAccount(req);
			GameInstanceInfo gii = handleAddGameInstance(gi, account);
			JSONUtils.sendGameInstance(resp, gii.gi, gii.gt, gii.gs);
		}catch (RequestException re) {
			resp.sendError(re.getErrorCode(), re.getMessage());
			return;
		}
	}
	
	
	public static GameInstanceInfo testHandleAddGameInstance(GameInstance gi,
			Account account) throws RequestException {
		return handleAddGameInstance(gi, account);
	}
	private static GameInstanceInfo handleAddGameInstance(GameInstance gi,
			Account account) throws RequestException {

		EntityManager em = EMF.get().createEntityManager();
		try {			
			GameServer gs = null;
			GameTemplate gt = null;
			if (gi.getKey()!=null) 
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "addGameInstance cannot have key specified");

			// not sure when to enforce this...
			if (gi.getGameServerId()!=null) {
				gs = em.find(GameServer.class, gi.getGameServerId());
				if (gs==null) {
					throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"Add GameInstance GameServer '"+gi.getGameServerId()+"' unknown");
				}
				if (!KeyFactory.keyToString(gs.getOwnerId()).equals(KeyFactory.keyToString(account.getKey()))) {
					throw new RequestException(HttpServletResponse.SC_FORBIDDEN,"Add GameInstance GameServer '"+gi.getGameServerId()+"' not owned by "+account.getNickname());
				}
			}
			if (gi.getGameTemplateId()==null) {
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"Add GameInstance must have gameTemplateId");
			}
			gt = em.find(GameTemplate.class, GameTemplate.idToKey(gi.getGameTemplateId()));
			if (gt==null) {
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"Add GameInstance GameTemplate '"+gi.getGameTemplateId()+"' unknown");
			}
			if (!KeyFactory.keyToString(gt.getOwnerId()).equals(KeyFactory.keyToString(account.getKey()))) {
				throw new RequestException(HttpServletResponse.SC_FORBIDDEN,"Add GameInstance GameTemplate '"+gi.getGameTemplateId()+"' not owned by "+account.getNickname());
			}
			// cannot add managed instances by hand - requires GameInstanceFactory
			gi.setStatus(GameInstanceStatus.UNMANAGED);

			// cache state
			if (gi.getMaxNumSlots()<=0)
				gi.setFull(true);

			em.persist(gi);
			logger.info("Creating GameInstance "+gi+" for Account "+account.getUserId()+" ("+account.getNickname()+")");

			return new GameInstanceInfo(gi, gt, gs);
		}
		finally {
			em.close();
		}
	}
}
