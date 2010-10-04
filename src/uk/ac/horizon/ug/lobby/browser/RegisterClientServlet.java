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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.protocol.ClientRequest;
import uk.ac.horizon.ug.lobby.protocol.ClientResponse;
import uk.ac.horizon.ug.lobby.protocol.ClientResponseStatus;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.protocol.RegisterClientRequest;
import uk.ac.horizon.ug.lobby.protocol.RegisterClientResponse;
import uk.ac.horizon.ug.lobby.protocol.RegisterClientResponseStatus;

/**
 * @author cmg
 *
 */
public class RegisterClientServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(RegisterClientServlet.class.getName());

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// parse request
		// NB this had BETTER be called over HTTPS
		String line = null;
		RegisterClientRequest rcreq = null;
		try {
			BufferedReader br = req.getReader();
			line = br.readLine();
			JSONObject json = new JSONObject(line);
			rcreq = JSONUtils.parseRegisterClientRequest(json);
			
			logger.info("RegisterClientRequest "+rcreq);
		}
		catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;			
		}
		try {			
			// app logic...
			RegisterClientResponse rcresp = handleRequest(rcreq);
			
			JSONUtils.sendRegisterClientResponse(resp, rcresp);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		}
	}

	/** test entry point */
	public static RegisterClientResponse testHandleRequest(RegisterClientRequest rcreq) throws RequestException {
		return handleRequest(rcreq);
	}

	/** handle Client registration request (esp. trying to set a sharedSecret for subsequent 
	 *  authentication).
	 *  
	 * @param rcreq
	 * @return
	 * @throws RequestException
	 */
	private static RegisterClientResponse handleRequest(RegisterClientRequest rcreq) throws RequestException {
		RegisterClientResponse rcresp = new RegisterClientResponse();
		rcresp.setVersion(RegisterClientRequest.VERSION);
		// validate request
		String clientId = rcreq.getClientId();
		if (clientId==null)
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"clientId not specified");
		String sharedSecret = rcreq.getSharedSecret();
		if (sharedSecret==null || sharedSecret.length()==0)
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,"sharedSecret not specified");

		// handle request
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		try {
			// prepare
			Key clientKey = GameClient.idToKey(clientId);
			
			et.begin();
			GameClient gc = em.find(GameClient.class, clientKey);
			if (gc==null) {
				// create client
				gc = new GameClient();
				gc.setKey(clientKey);
				em.persist(gc);
				logger.info("Creating GameClient "+clientId+" on RegisterClient");
			}
			
			if (gc.getSharedSecret()==null || gc.getSharedSecret().equals(sharedSecret)) {
				// OK
				gc.setSharedSecret(sharedSecret);
				// update properties
				if (rcreq.getClientType()!=null)
					gc.setClientType(rcreq.getClientType());
				if (rcreq.getMajorVersion()!=null) 
					gc.setMajorVersion(rcreq.getMajorVersion());
				if (rcreq.getMinorVersion()!=null) 
					gc.setMinorVersion(rcreq.getMinorVersion());
				if (rcreq.getUpdateVersion()!=null) 
					gc.setUpdateVersion(rcreq.getUpdateVersion());
				if (rcreq.getNickname()!=null) 
					gc.setNickname(rcreq.getNickname());
				et.commit();
				
				logger.info("Set/updated GameClient on Register: "+gc);
				
				rcresp.setStatus(RegisterClientResponseStatus.OK);
				return rcresp;
			}
			
			// no...
			et.rollback();
			rcresp.setStatus(RegisterClientResponseStatus.ERROR_NOT_PERMITTED);
			logger.info("Failed/Not permitted RegisterClient "+clientId);
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
		return rcresp;
	}
	
}
