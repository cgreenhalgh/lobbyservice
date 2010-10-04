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
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.AccountAuditRecordType;
import uk.ac.horizon.ug.lobby.model.AuditRecordLevel;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientStatus;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.protocol.ClientManagementRequest;
import uk.ac.horizon.ug.lobby.protocol.ClientManagementResponse;
import uk.ac.horizon.ug.lobby.protocol.ClientManagementResponseStatus;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequest;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequestType;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.server.AuditUtils;
import uk.ac.horizon.ug.lobby.user.AddGameInstanceServlet.GameInstanceInfo;

/** 
 * Get all GameClients (user view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class ClientManagementServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(ClientManagementServlet.class.getName());
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
        ClientManagementRequest cmreq = null;
		try {
			BufferedReader r = req.getReader();
			String line = r.readLine();
			if (line==null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No data supplied");
				return;				
			}
			// why does this seem to read {} ??
			//JSONObject json = new JSONObject(req.getReader());
			JSONObject json = new JSONObject(line);
			cmreq = JSONUtils.parseClientManagementRequest(json);
		}
		catch (JSONException je) {
			logger.warning("Bad request: "+je.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, je.toString());
			return;
		}
        
		try {
			Account account = AccountUtils.getAccount(req);
			ClientManagementResponse cmresp = handleClientManagement(cmreq, account, req.getRemoteAddr());
			logger.info("Handled "+cmreq+" -> "+cmresp);
			JSONUtils.sendClientManagementResponse(resp, cmresp);
		}catch (RequestException re) {
			logger.warning("Error: "+re.getErrorCode()+": "+re.getMessage());
			resp.sendError(re.getErrorCode(), re.getMessage());
			return;
		}

	}

	private static ClientManagementResponse handleClientManagement(
			ClientManagementRequest cmreq, Account account, String clientIp) throws RequestException {
		// validate request
		String clientId = cmreq.getClientId();
		if (clientId==null) 
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "clientId not specified");
		GameClientStatus newStatus = cmreq.getNewStatus();
		if (newStatus==null) 
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "newStatus not specified");
		if (newStatus!=GameClientStatus.TRUSTED && newStatus!=GameClientStatus.BLOCKED) 
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "Cannot change status to "+newStatus);

		// TODO authenticate client request (clientHmac, clientTime)!
		
		ClientManagementResponse cmresp = new ClientManagementResponse();
		cmresp.setClientId(clientId);
		cmresp.setVersion(ClientManagementRequest.VERSION);

		// get client, ready to update atomically
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		try {
			// key 
			Key clientKey = GameClient.idToKey(clientId);
			et.begin();
			
			GameClient gc = em.find(GameClient.class, clientKey);
			if (gc==null) {
				cmresp.setStatus(ClientManagementResponseStatus.ERROR_UNKNOWN_CLIENT);
				cmresp.setMessage("Client is unknown");
				return cmresp;
			}
			
			if (gc.getAccountKey()!=null && !gc.getAccountKey().equals(account.getKey())) {
				cmresp.setStatus(ClientManagementResponseStatus.ERROR_CLIENT_NOT_PERMITTED);
				cmresp.setMessage("Client is owned by another user");
				return cmresp;
			}
			else if (gc.getAccountKey()==null) {
 				// fix?
				gc.setStatus(GameClientStatus.ANONYMOUS);
			}			
			
			if (cmreq.getNewStatus()==gc.getStatus()) {
				logger.info("Client status unchanged ("+gc.getStatus()+")");
				// no op
				cmresp.setStatus(ClientManagementResponseStatus.OK);
				return cmresp;
			}
			logger.info("Client status "+gc.getStatus()+" -> "+cmreq.getNewStatus());
			
			if (gc.getStatus()==GameClientStatus.BLOCKED) {
				// can't do anything to a BLOCKED client
				cmresp.setStatus(ClientManagementResponseStatus.ERROR_CLIENT_BLOCKED);
				cmresp.setMessage("Client is already BLOCKED");
				return cmresp;
			}
			
			// update
			long now = System.currentTimeMillis();
			gc.setStatus(cmreq.getNewStatus());
			gc.setAccountKey(account.getKey());
			if (gc.getStatus()==GameClientStatus.TRUSTED)
				gc.setTrustedFromTime(now);
			else if (gc.getStatus()==GameClientStatus.BLOCKED)
				gc.setTrustedToTime(now);
			// commit!
			et.commit();

			// audit
			if (gc.getStatus()==GameClientStatus.TRUSTED)				
				AuditUtils.logAccountAuditRecord(gc.getKey(), account.getKey(), clientIp, now, AccountAuditRecordType.USER_TRUSTED_CLIENT, AuditRecordLevel.NORMAL, /*detailsJson*/"{}", "User trusted client "+clientId);
			else if (gc.getStatus()==GameClientStatus.BLOCKED)				
				AuditUtils.logAccountAuditRecord(gc.getKey(), account.getKey(), clientIp, now, AccountAuditRecordType.USER_BLOCKED_CLIENT, AuditRecordLevel.NORMAL, /*detailsJson*/"{}", "User trusted client "+clientId);
			 
			cmresp.setStatus(ClientManagementResponseStatus.OK);
			return cmresp;
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
	}
}
