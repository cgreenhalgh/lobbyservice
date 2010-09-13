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

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class AddGameServerServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(AddGameServerServlet.class.getName());
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
        GameServer gs = null;
		try {
			BufferedReader r = req.getReader();
			String line = r.readLine();
			// why does this seem to read {} ??
			//JSONObject json = new JSONObject(req.getReader());
			JSONObject json = new JSONObject(line);
			gs = JSONUtils.parseGameServer(json);
		}
		catch (JSONException je) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, je.toString());
			return;
		}
        
		try {
			Account account = AccountUtils.getAccount(req);
			handleAddGameServer(gs, account);
			JSONUtils.sendGameServer(resp, gs);
		
		} catch (RequestException re) {
			resp.sendError(re.getErrorCode(), re.getMessage());
			return;
		}
	}

	public static void testHandleAddGameServer(GameServer gs, Account account) throws RequestException {
		handleAddGameServer(gs, account);
	}
	
	private static void handleAddGameServer(GameServer gs, Account account) throws RequestException {
		if (account.getGameTemplateQuota()<=0) {
			// can't have servers if don't have templates
			String msg = "Account "+account.getUserId()+" ("+account.getNickname()+") cannot add GameServer: quota=0";
			logger.info(msg);
			throw new RequestException(HttpServletResponse.SC_FORBIDDEN, msg);
		}
		if (gs.getKey()!=null) 
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "addGameServer cannot have key specified");

		EntityManager em = EMF.get().createEntityManager();
		try {
			// fill in missing info
			gs.setOwnerId(account.getKey());
			gs.setLastKnownStatus(GameServerStatus.UNKNOWN);
			gs.setLastKnownStatusTime(System.currentTimeMillis());

			em.persist(gs);
			logger.info("Creating GameServer "+gs+" for Account "+account.getUserId()+" ("+account.getNickname()+")");
		}
		finally {
			em.close();
		}

	}
}
