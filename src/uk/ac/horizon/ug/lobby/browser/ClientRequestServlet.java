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
import uk.ac.horizon.ug.lobby.protocol.ClientRequest;
import uk.ac.horizon.ug.lobby.protocol.ClientRequestScope;
import uk.ac.horizon.ug.lobby.protocol.ClientRequestType;
import uk.ac.horizon.ug.lobby.protocol.ClientResponse;
import uk.ac.horizon.ug.lobby.protocol.ClientResponseStatus;
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
 * Handle request a ClientRequest, initially LIST_GAMES.
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class ClientRequestServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(ClientRequestServlet.class.getName());

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		// parse request
		String line = null;
		String auth = null;
		ClientRequest creq = null;
		try {
			BufferedReader br = req.getReader();
			line = br.readLine();
			JSONObject json = new JSONObject(line);
			creq = JSONUtils.parseClientRequest(json);
			// second line is digital signature (if given)
			auth = br.readLine();
			
			logger.info("ClientRequest "+creq);
		}
		catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;			
		}
		try {			
			// app logic...
			ClientResponse cresp = handleRequest(creq, req.getRequestURI(), line, auth);
			
			JSONUtils.sendClientResponse(resp, cresp);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JoinException e) {
			ClientResponse cresp = new ClientResponse();
			try {
				cresp.setStatus(ClientResponseStatus.valueOf(e.getStatus().toString()));
			}
			catch (Exception e2) {
				logger.warning("Cannot map JoinException "+e.getStatus()+" to ClientResponseStatus: "+e2);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Mapping error return "+e.getStatus());
				return;
			}
			cresp.setTime(System.currentTimeMillis());
			//cresp.setType(gjreq.getType());
			cresp.setMessage(e.getMessage());
			logger.warning(e.toString());
			JSONUtils.sendClientResponse(resp, cresp);
			return;
		}
	}

	private ClientResponse handleRequest(ClientRequest creq, String requestUri, String line,
			String auth) throws IOException, JoinException, RequestException {
		// authenticate 
		JoinUtils.JoinAuthInfo jai = JoinUtils.authenticate(creq.getClientId(), null, null, true, requestUri, line, auth);			

		ClientResponse cresp = null;
		switch (creq.getType()) {
		case LIST_GAMES:
			cresp = handleListGames(creq, jai.gc, jai.account);
			break;				
		default:
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "Request type must be LIST_GAMES ("+creq.getType()+")");
		}
		return cresp;
	}

	private ClientResponse handleListGames(ClientRequest creq, GameClient gc, Account account) throws JoinException {
		ClientResponse cresp = new ClientResponse();
		cresp.setTime(System.currentTimeMillis());
		// hopefully :)
		cresp.setStatus(ClientResponseStatus.OK);

		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		
		// need to get suitable games...
		List<GameTemplateInfo> gtis = new LinkedList<GameTemplateInfo>();
		// TODO filter / structure request to cope with large numbers of slots ?!
		// TODO map status to slot to allow filter in query
		EntityManager em = EMF.get().createEntityManager();
		// readonly - no transaction?!
		try {
			Query q = null;
			if (creq.getScope()==null) {
				// default to CLIENT
				creq.setScope(ClientRequestScope.CLIENT);
			}
			switch (creq.getScope()) {
			case ACCOUNT:
				if (account==null) 
					throw new JoinException(GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "Account-scope query requires user authentication");
				q = em.createQuery("SELECT x FROM "+GameInstanceSlot.class.getSimpleName()+" x WHERE x."+ACCOUNT_KEY+" = :"+ACCOUNT_KEY);
				q.setParameter(ACCOUNT_KEY, account.getKey());
				break;
			case CLIENT:
				if (gc==null) 
					throw new JoinException(GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Client-scope query requires client authentication");
				q = em.createQuery("SELECT x FROM "+GameInstanceSlot.class.getSimpleName()+" x WHERE x."+GAME_CLIENT_KEY+" = :"+GAME_CLIENT_KEY);
				q.setParameter(GAME_CLIENT_KEY, gc.getKey());
				break;
			}
			List<GameInstanceSlot> posgiss = (List<GameInstanceSlot>)q.getResultList();
			for (GameInstanceSlot posgis: posgiss) {
				if (posgis.getGameInstanceKey()==null) {
					logger.warning("GameInstanceSlot without gameInstanceKey: "+posgis);
					continue;
				}
				GameInstance gi = em.find(GameInstance.class, posgis.getGameInstanceKey());
				if (gi==null) {
					logger.warning("Could not find GameInstance for slot: "+posgis);
					continue;
				}
				boolean include = false;
				// include?
				switch (gi.getNominalStatus()) {
				case CANCELLED:
				case ENDED:
					if (creq.getIncludeEnded()!=null || creq.getIncludeEnded()==true)
						include = true;
					break;
				case PLANNED:
					if (creq.getIncludePlanned()!=null || creq.getIncludePlanned()==true)
						include = true;
					break;
				case TEMPORARILY_UNAVAILABLE:
				case AVAILABLE:
					if (creq.getIncludeAvailable()!=null || creq.getIncludeAvailable()==true)
						include = true;
					break;
				}
				
				if (include) {
					GameTemplateInfo gti = new GameTemplateInfo(); 
					gti.setGameInstance(gi);
					GameTemplate gt = em.find(GameTemplate.class, GameTemplate.idToKey(gi.getGameTemplateId()));
					gti.setGameTemplate(gt);
					// GameClientTemplates??
					gti.setJoinUrl(QueryGameTemplateServlet.makeJoinUrl(sc, gi));
					gtis.add(gti);
				}
			}
		}
		finally {
			em.close();
		}
		
		cresp.setGames(gtis);
		return cresp;
	}
}
