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
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientStatus;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.protocol.ClientRequirement;
import uk.ac.horizon.ug.lobby.protocol.ClientRequirementFailureType;
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
		public String characteristicsJson;
		/** cons */
		public ClientInfo() {}
		/**
		 * @param characteristicsJson
		 */
		public ClientInfo(String characteristicsJson) {
			super();
			this.characteristicsJson = characteristicsJson;
		}
		/**
		 * @return the characteristicsJson
		 */
		public String getCharacteristicsJson() {
			return characteristicsJson;
		}
		/**
		 * @param characteristicsJson the characteristicsJson to set
		 */
		public void setCharacteristicsJson(String characteristicsJson) {
			this.characteristicsJson = characteristicsJson;
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
	public static JoinAuthInfo authenticateOptional(String clientId, String deviceId, String requestUri, String line, String auth) {
		try {
			return authenticateInternal(clientId, deviceId, null, true, requestUri, line, auth, true);
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
	public static JoinAuthInfo authenticate(String clientId, String deviceId, ClientInfo clientInfo, boolean allowAnonymousClients, String requestUri, String line, String auth) throws IOException, JoinException, RequestException {
		return authenticateInternal(clientId, deviceId, clientInfo, allowAnonymousClients, requestUri, line, auth, false);
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
	private static JoinAuthInfo authenticateInternal(String clientId, String deviceId, ClientInfo clientInfo, boolean allowAnonymousClients, String requestUri, String line, String auth, boolean optional) throws IOException, JoinException, RequestException {

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
				if (!authenticateRequest(requestUri, line, gc, account, auth)) {
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
		return toHex(bytes);
	}
	public static String toHex(byte bytes[]) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<bytes.length; i++) {
			sb.append(nibble((bytes[i] >> 4) & 0xf));
			sb.append(nibble(bytes[i] & 0xf));
		}
		return sb.toString();
	}

	public static char nibble(int i) {
		if (i<10)
			return (char)('0'+i);
		else 
			return (char)('a'+i-10);
	}

	public static byte[] parseHex(String sharedSecretHex){
		byte data[] = new byte[(sharedSecretHex.length()+1)/2];
		for (int i=0; i<sharedSecretHex.length(); i++) {
			int nibble = 0;
			char c = sharedSecretHex.charAt(i);
			if (c>='0' && c<='9')
				nibble = (int)(c-'0');
			else if (c>='a' && c<='f')
				nibble = (int)(10+c-'a');
			else if (c>='A' && c<='F')
				nibble = (int)(10+c-'A');
			if ((i&1)==0)
				data[i/2] = (byte)(/*data[i/2] | */(nibble << 4));
			else
				data[i/2] = (byte)(data[i/2] | (nibble));
		}
		return data;
	}
	
	public static boolean authenticateRequest(String requestUri, String line, GameClient gc,
			Account account, String auth) {
		logger.warning("Authenticate "+line+" with "+auth+" for "+gc+" ("+account+")");
		// v.1 HMAC-SHA1 of bytes of line (UTF-8)
		if (auth==null || auth.length()==0)
		{
			logger.warning("Authenticate with no auth line");
			return false;
		}
		String sharedSecret = gc.getSharedSecret();
		if (sharedSecret==null || sharedSecret.length()==0) {
			logger.warning("Authenticate with no sharedSecret for "+gc);
			return false;			
		}
		byte[] keyBytes = parseHex(sharedSecret);
		SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA1");
		//logger.info("sharedSecret="+sharedSecret+" ("+toHex(keyBytes)+"), key="+key);
		Mac m;
		try {
			m = Mac.getInstance("HmacSHA1");
			m.init(key);
			// URI should already be %escaped?
			m.update(requestUri.getBytes(Charset.forName("ASCII")));
			m.update((byte)0);
			m.update(line.getBytes(Charset.forName("UTF-8")));
			byte[] mac = m.doFinal();
			String smac = toHex(mac);
			
			if (smac.equals(auth)) {
				return true;
			}
			logger.warning("HMAC did not match: "+auth+" vs "+smac);
			return false;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error generating HMAC", e);
		}
		return false;
	}

	/** create and persist a new anonymous GameClient within calling transaction (for consistency) */
	private static GameClient createAnonymousClient(EntityManager em, String clientId, ClientInfo clientInfo) {
		GameClient gc = new GameClient();
		gc.setCreatedTime(System.currentTimeMillis());
		gc.setStatus(GameClientStatus.ANONYMOUS);
		if (clientId==null)
			clientId = GUIDFactory.newGUID();
		gc.setId(clientId);
		//gc.setKey(GameClient.idToKey(null, clientId));
		if (clientInfo.getCharacteristicsJson()!=null) {
			gc.setCharacteristicsJson(clientInfo.getCharacteristicsJson());
		}
		// nickname only for slot, not client
		//EntityTransaction et = em.getTransaction();
		em.persist(gc);
		logger.info("Created anonymous client "+clientId);
		return gc;
	}

	public static List<GameClientTemplate> getGameClientTemplates(GameJoinRequest gjreq, String gameTemplateId) {
		return QueryGameTemplateServlet.getGameClientTemplates(gjreq.getClientTitle(), gjreq.getCharacteristicsJson(), gameTemplateId);
	}
	public static enum SatisfiesClientRequirements {
		YES, NO, MAYBE
	}
	public static SatisfiesClientRequirements satisfiesClientRequirements (
			JSONObject characteristics, List<ClientRequirement> crs) {
		boolean uncertain = false;
		for (ClientRequirement cr : crs) {
			String key = cr.getCharacteristic();
			boolean satisfied = false;
			if (characteristics.has(key)) {
				try {
					String value = characteristics.get(key).toString();
					String exp = cr.getExpression();
					satisfied = satisfiesConstraint(value, exp);
				} catch (JSONException e) {
					logger.log(Level.WARNING, "Characteristics missing expected key "+key+" - should not happen");
				}
			}
			else {
				if ("UNDEFINED".equalsIgnoreCase(cr.getExpression()))
					// OK!
					satisfied = true;
			}
			if (!satisfied) {
				switch (cr.getFailure()) {
				case Fail:
					return SatisfiesClientRequirements.NO;
				case Continue:
					break;
				case Recheck:
					uncertain = true;
					break;
				}
			}
		}
		if (uncertain)
			return SatisfiesClientRequirements.MAYBE;
		return SatisfiesClientRequirements.YES;
	}
	private static boolean satisfiesConstraint(String value, String exp) {
		if ("UNDEFINED".equalsIgnoreCase(exp))
			return value==null;
		if ("TRUE".equalsIgnoreCase(exp)) {
			return "TRUE".equalsIgnoreCase(value);
		}
		if ("FALSE".equalsIgnoreCase(exp)) {
			return !"TRUE".equalsIgnoreCase(value);
		}
		if (exp.startsWith("="))
			return exp.substring(1).trim().equals(value);
		if (exp.startsWith("IN")) {
			String options [] = exp.substring(2).split("[(,]");
			for (int i=0; i<options.length; i++) {
				if (options[i].equals(value))
					return true;
			}
			return false;
		}
		if (exp.startsWith(">=")) {
			try {
				double dval = Double.parseDouble(value);
				double eval = Double.parseDouble(exp.substring(2).trim());
				return dval >= eval;
			}
			catch (NumberFormatException nfe) {
				logger.log(Level.WARNING, "satisfiesConstraint "+value+" vs "+exp, nfe);
				return false;
			}
		}
		if (exp.startsWith("<=")) {
			try {
				double dval = Double.parseDouble(value);
				double eval = Double.parseDouble(exp.substring(2).trim());
				return dval <= eval;
			}
			catch (NumberFormatException nfe) {
				logger.log(Level.WARNING, "satisfiesConstraint "+value+" vs "+exp, nfe);
				return false;
			}
		}
		if (exp.startsWith("LIKE")) {
			exp = exp.substring(4).replace("()","  ").trim();
			boolean wildcardAtStart = exp.startsWith("%");
			boolean wildcardAtEnd = exp.endsWith("%");
			exp = exp.substring(wildcardAtStart ? 1 : 0, exp.length()-(wildcardAtStart ? 1 : 0)-(wildcardAtEnd ? 1 : 0));
			if (wildcardAtStart && wildcardAtEnd) {
				return value.contains(exp);
			}
			if (wildcardAtStart && !wildcardAtEnd) {
				return value.endsWith(exp);				
			}
			if (!wildcardAtStart && wildcardAtEnd) {
				return value.startsWith(exp);
			}
			return value.equals(exp);
		}
		logger.log(Level.WARNING,"Unsupported ClientRequirement expression: "+exp);
		return false;
	}
}
