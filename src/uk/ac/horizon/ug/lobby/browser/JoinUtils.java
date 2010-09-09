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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequest;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponse;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponseStatus;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/**
 * @author cmg
 *
 */
public class JoinUtils implements Constants {
	static Logger logger = Logger.getLogger(JoinUtils.class.getName());
	/** info from authenticate */
	public static class JoinAuthInfo {
		public GameClient gc = null;
		public Account account = null;
		public boolean anonymous = false;
	}
	/** initial check/authenticate join request.
	 * @return null if not permitted (response sent) 
	 * @throws IOException */
	public static JoinAuthInfo authenticate(GameJoinRequest gjreq, GameJoinResponse gjresp, boolean allowAnonymousClients, HttpServletResponse resp, String line, String auth) throws IOException {
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			GameClient gc = null;
			Account account = null;
			boolean anonymous = false;
			if (gjreq.getClientId()==null) {
				// anonymous attempt
				if (!allowAnonymousClients) {
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game does not allow anonymous players");
					return null;
				}
				if (gjreq.getGameSlotId()!=null){
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Changing an existing game slot requires a client to be identified");
					return null;
				}
				// ensure possible createAnonymousClient will be atomic wrt to the next check...
				et.rollback();
				et.begin();
				
				// does default client already exist?
				String clientId = gjreq.getDeviceId();
				if (clientId!=null) {
					Key clientKey = GameClient.idToKey(clientId);
					gc = em.find(GameClient.class, clientKey);
					if (gc.getAccountKey()!=null || gc.getSharedSecret()!=null) {
						logger.warning("Client deviceId="+gjreq.getDeviceId()+" already exists, non-anonymous");
						gc = null;
						clientId = null;
					}
					else
						logger.info("Using default anonymous client with deviceId="+gjreq.getDeviceId());
				}

				if (gc==null) {

					// create is done in our transaction
					gc = createAnonymousClient(em, gjreq, clientId);
				}

				// COMMIT!
				et.commit();
				et.begin();
				anonymous = true;
			}
			else {
				// identified client 
				String clientId = gjreq.getClientId();
				Key clientKey = GameClient.idToKey(clientId);
				gc = em.find(GameClient.class, clientKey);
				if (gc==null) {
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_AUTHENTICATION_FAILED, "GameClient "+clientId+" unknown");
					return null;
				}
				et.rollback();
				et.begin();

				if (gc.getAccountKey()!=null) {
					account = em.find(Account.class, gc.getAccountKey());
					if (account==null) {
						logger.warning("GameClient "+clientId+" found but Account missing: "+gc);
						sendError(resp, gjresp, GameJoinResponseStatus.ERROR_AUTHENTICATION_FAILED, "This clientId is not usable");
						return null;						
					}
				}
				else {
					if (!allowAnonymousClients) {
						sendError(resp, gjresp, GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game does not allow anonymous players");
						return null;
					}
					anonymous = true;
				}
				// authenticate
				if (!authenticateRequest(line, gc, account, auth)) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication failed");
					return null;					
				}
			}
			gjresp.setClientId(gc.getId());

			JoinAuthInfo jai = new JoinAuthInfo();
			jai.account = account;
			jai.anonymous = anonymous;
			jai.gc = gc;
			return jai;
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
	}
	/** default poll delay (30s?!) */
	public static final int DEFAULT_POLL_INTERVAL_MS = 30000;

	public static void setTryLater(GameJoinResponse gjresp) {
		gjresp.setPlayTime(System.currentTimeMillis()+DEFAULT_POLL_INTERVAL_MS);
		setError(gjresp, GameJoinResponseStatus.TRY_LATER, "The game is not available right now - please try again in a minute");
	}

	/** send 'error' in our GameJoinResponse (i.e. not HTTP error) */
	public static void sendError(HttpServletResponse resp, GameJoinResponse gjresp, GameJoinResponseStatus errorStatus) throws IOException {
		// TODO user friendly
		sendError(resp, gjresp, errorStatus, errorStatus.name());
	}
	/** send 'error' in our GameJoinResponse (i.e. not HTTP error) */
	public static void sendError(HttpServletResponse resp, GameJoinResponse gjresp, GameJoinResponseStatus errorStatus, String message) throws IOException {
		gjresp.setStatus(errorStatus);
		gjresp.setMessage(message);
		logger.warning("Sending error response: "+gjresp);
		JSONUtils.sendGameJoinResponse(resp, gjresp);
	}
	/** set 'error' in our GameJoinResponse (i.e. not HTTP error) */
	public static void setError(GameJoinResponse gjresp, GameJoinResponseStatus errorStatus, String message) {
		gjresp.setStatus(errorStatus);
		gjresp.setMessage(message);
		logger.warning("Setting error response: "+gjresp);
	}
	private static SecureRandom secureRandom;
	private static Random random;
	private static final int DEFAULT_SHARED_SECRET_BITS = 128;
	
	public static synchronized String createClientSharedSecret() {
		return createClientSharedSecret(DEFAULT_SHARED_SECRET_BITS);
	}
	public static synchronized String createClientSharedSecret(int bits) {
		if (secureRandom==null && random==null) {
			try {
				secureRandom = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				logger.warning("Could not create SecureRandom: "+e.toString());
				random = new Random(System.currentTimeMillis() ^ e.hashCode());
			}
		}
		byte bytes[] = new byte[(bits+7)/8];
		if (secureRandom!=null) {
			secureRandom.nextBytes(bytes);
		}
		else 
			random.nextBytes(bytes);
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<bytes.length; i++) {
			sb.append(nibble(bytes[i] & 0xf));
			sb.append(nibble((bytes[i] >> 4) & 0xf));
		}
		return sb.toString();
	}

	public static char nibble(int i) {
		if (i<10)
			return (char)('0'+i);
		else 
			return (char)('a'+i-10);
	}

	public static boolean authenticateRequest(String line, GameClient gc,
			Account account, String auth) {
		logger.warning("Authenticate "+line+" with "+auth+" for "+gc+" ("+account+")");
		// TODO
		return true;
	}

	public static GameClient createAnonymousClient(EntityManager em, GameJoinRequest gjreq, String clientId) {
		GameClient gc = new GameClient();
		if (clientId==null)
			clientId = GUIDFactory.newGUID();
		gc.setId(clientId);
		//gc.setKey(GameClient.idToKey(null, clientId));
		if (gjreq.getClientType()!=null)
			gc.setClientType(gjreq.getClientType());
		if (gjreq.getMajorVersion()!=null)
			gc.setMajorVersion(gjreq.getMajorVersion());
		if (gjreq.getMinorVersion()!=null)
			gc.setMinorVersion(gjreq.getMinorVersion());
		if (gjreq.getUpdateVersion()!=null)
			gc.setUpdateVersion(gjreq.getUpdateVersion());
		// nickname only for slot, not client
		//EntityTransaction et = em.getTransaction();
		em.persist(gc);
		logger.info("Created anonymous client "+clientId);
		return gc;
	}

	public static List<GameClientTemplate> getGameClientTemplates(EntityManager em,
			GameJoinRequest gjreq, String gameTemplateId) {
		return QueryGameTemplateServlet.getGameClientTemplates(em, gjreq.getClientTitle(), gjreq.getClientType(), gameTemplateId, gjreq.getMajorVersion(), gjreq.getMinorVersion(), gjreq.getUpdateVersion());
	}
}
