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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
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
	/** client info - for create anon */
	public static class ClientInfo {
		public String clientType;
		public Integer majorVersion;
		public Integer minorVersion;
		public Integer updateVersion;
		/** cons */
		public ClientInfo() {}
		
		/**
		 * @param string
		 * @param majorVersion
		 * @param minorVersion
		 * @param updateVersion
		 */
		public ClientInfo(String string, Integer majorVersion,
				Integer minorVersion, Integer updateVersion) {
			super();
			this.clientType = string;
			this.majorVersion = majorVersion;
			this.minorVersion = minorVersion;
			this.updateVersion = updateVersion;
		}

		/**
		 * @return the clientType
		 */
		public String getClientType() {
			return clientType;
		}
		/**
		 * @param clientType the clientType to set
		 */
		public void setClientType(String clientType) {
			this.clientType = clientType;
		}
		/**
		 * @return the majorVersion
		 */
		public Integer getMajorVersion() {
			return majorVersion;
		}
		/**
		 * @param majorVersion the majorVersion to set
		 */
		public void setMajorVersion(Integer majorVersion) {
			this.majorVersion = majorVersion;
		}
		/**
		 * @return the minorVersion
		 */
		public Integer getMinorVersion() {
			return minorVersion;
		}
		/**
		 * @param minorVersion the minorVersion to set
		 */
		public void setMinorVersion(Integer minorVersion) {
			this.minorVersion = minorVersion;
		}
		/**
		 * @return the updateVersion
		 */
		public Integer getUpdateVersion() {
			return updateVersion;
		}
		/**
		 * @param updateVersion the updateVersion to set
		 */
		public void setUpdateVersion(Integer updateVersion) {
			this.updateVersion = updateVersion;
		}
	}
	/** initial check/authenticate join request - where optional.
	 * Never creates Clients - GameClient return may be null.
	 * @param clientId The ID of the GameClient (from the request)
	 * @param deviceId The (optional) ID of the device, for use as fallback default clientId
	 * @param link The text of the request line (for request authentication)
	 * @param auth The text of the request signature line (for request authentication)
	 * @param clientInfo ClientInfo for create new anonymous client (optional); null if not to create
	 * @return null if not permitted (response sent) */
	public static JoinAuthInfo authenticateOptional(String clientId, String deviceId, String line, String auth) {
		try {
			return authenticateInternal(clientId, deviceId, null, true, line, auth, true);
		} catch (Exception e) {
			logger.log(Level.WARNING,"Problem doing authenticateOptional - fall back to unknown", e);
			JoinAuthInfo jai = new JoinAuthInfo();
			jai.anonymous = true;
			return jai;
		}
	}
	/** initial check/authenticate join request.
	 * @param clientId The ID of the GameClient (from the request)
	 * @param deviceId The (optional) ID of the device, for use as fallback default clientId
	 * @param link The text of the request line (for request authentication)
	 * @param auth The text of the request signature line (for request authentication)
	 * @param clientInfo ClientInfo for create new anonymous client (optional); null if not to create
	 * @return null if not permitted (response sent) 
	 * @throws IOException 
	 * @throws RequestException */
	public static JoinAuthInfo authenticate(String clientId, String deviceId, ClientInfo clientInfo, boolean allowAnonymousClients, String line, String auth) throws IOException, JoinException, RequestException {
		return authenticateInternal(clientId, deviceId, clientInfo, allowAnonymousClients, line, auth, false);
	}
	/** initial check/authenticate join request.
	 * @param clientId The ID of the GameClient (from the request)
	 * @param deviceId The (optional) ID of the device, for use as fallback default clientId
	 * @param link The text of the request line (for request authentication)
	 * @param auth The text of the request signature line (for request authentication)
	 * @param clientInfo ClientInfo for create new anonymous client (optional); null if not to create
	 * @return null if not permitted (response sent) 
	 * @throws IOException 
	 * @throws RequestException */
	private static JoinAuthInfo authenticateInternal(String clientId, String deviceId, ClientInfo clientInfo, boolean allowAnonymousClients, String line, String auth, boolean optional) throws IOException, JoinException, RequestException {

		// default
		JoinAuthInfo jai = new JoinAuthInfo();
		jai.anonymous = true;
		
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			GameClient gc = null;
			Account account = null;
			boolean anonymous = false;
			if (clientId==null) {
				// anonymous attempt
				if (!allowAnonymousClients) {
					if (optional)
						// unknown
						return jai;
					throw new JoinException(GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game does not allow anonymous players");
				}
				// ensure possible createAnonymousClient will be atomic wrt to the next check...
				et.rollback();
				et.begin();
				
				// does default client already exist?
				if (deviceId!=null) {
					clientId = deviceId;
					Key clientKey = GameClient.idToKey(clientId);
					gc = em.find(GameClient.class, clientKey);
					if (gc!=null) {
						if (gc.getAccountKey()!=null || gc.getSharedSecret()!=null) {
							logger.warning("Client deviceId="+deviceId+" already exists, non-anonymous");
							if (optional)
								// fallback to unknown
								return jai;
							throw new JoinException(GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "This deviceId is already in use as an authenticated client");
						}
						else {
							logger.info("Using default anonymous client with deviceId="+deviceId);
							// existing anonymous
						}
					}
				}

				if (gc==null) {

					if (clientInfo==null || optional) {
						if (optional)
							// unknown
							return jai;
						// implies do not create
						throw new JoinException(GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Anonymous client does not exist");
					}
					// won't be optional if we got here
					// create is done in our transaction
					gc = createAnonymousClient(em, clientId, clientInfo);
				}

				// COMMIT!
				et.commit();
				et.begin();
				anonymous = true;
				// anonymous client...
			}
			else {
				// identified client 
				Key clientKey = GameClient.idToKey(clientId);
				gc = em.find(GameClient.class, clientKey);
				if (gc==null) {
					if (optional)
						// unknown
						return jai;
					throw new JoinException(GameJoinResponseStatus.ERROR_AUTHENTICATION_FAILED, "GameClient "+clientId+" unknown");
				}
				et.rollback();
				et.begin();

				if (gc.getAccountKey()!=null) {
					account = em.find(Account.class, gc.getAccountKey());
					if (account==null) {
						logger.warning("GameClient "+clientId+" found but Account missing: "+gc);
						if (optional)
							// unknown
							return jai;
						throw new JoinException(GameJoinResponseStatus.ERROR_AUTHENTICATION_FAILED, "This clientId is not usable");
					}
				}
				else {
					if (!allowAnonymousClients) {
						if (optional) 
							// fall back to unknown
							return jai;
						throw new JoinException(GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game does not allow anonymous players");
					}
					anonymous = true;
				}
				// authenticate
				if (!authenticateRequest(line, gc, account, auth)) {
					if (optional) 
						// fall back to unknown
						return jai;
					
					throw new RequestException(HttpServletResponse.SC_FORBIDDEN, "Authentication failed");
				}
				// authenticated with gc and/or account...
			}

			jai.account = account;
			jai.anonymous = anonymous;
			jai.gc = gc;
			return jai;
		}
		// throws JoinException
		// throws RequestException
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
	private static void sendError(HttpServletResponse resp, GameJoinResponse gjresp, GameJoinResponseStatus errorStatus) throws IOException {
		// TODO user friendly
		sendError(resp, gjresp, errorStatus, errorStatus.name());
	}
	/** send 'error' in our GameJoinResponse (i.e. not HTTP error) */
	private static void sendError(HttpServletResponse resp, GameJoinResponse gjresp, GameJoinResponseStatus errorStatus, String message) throws IOException {
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

	/** create and persist a new anonymous GameClient within calling transaction (for consistency) */
	private static GameClient createAnonymousClient(EntityManager em, String clientId, ClientInfo clientInfo) {
		GameClient gc = new GameClient();
		if (clientId==null)
			clientId = GUIDFactory.newGUID();
		gc.setId(clientId);
		//gc.setKey(GameClient.idToKey(null, clientId));
		if (clientInfo.getClientType()!=null)
			gc.setClientType(clientInfo.getClientType());
		if (clientInfo.getMajorVersion()!=null)
			gc.setMajorVersion(clientInfo.getMajorVersion());
		if (clientInfo.getMinorVersion()!=null)
			gc.setMinorVersion(clientInfo.getMinorVersion());
		if (clientInfo.getUpdateVersion()!=null)
			gc.setUpdateVersion(clientInfo.getUpdateVersion());
		// nickname only for slot, not client
		//EntityTransaction et = em.getTransaction();
		em.persist(gc);
		logger.info("Created anonymous client "+clientId);
		return gc;
	}

	public static List<GameClientTemplate> getGameClientTemplates(GameJoinRequest gjreq, String gameTemplateId) {
		return QueryGameTemplateServlet.getGameClientTemplates(gjreq.getClientTitle(), gjreq.getClientType(), gameTemplateId, gjreq.getMajorVersion(), gjreq.getMinorVersion(), gjreq.getUpdateVersion());
	}
}
