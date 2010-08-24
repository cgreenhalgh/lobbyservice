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
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceSlot;
import uk.ac.horizon.ug.lobby.model.GameInstanceSlotStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequest;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequestType;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponse;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponseStatus;
import uk.ac.horizon.ug.lobby.protocol.GameQuery;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.protocol.LocationConstraint;
import uk.ac.horizon.ug.lobby.protocol.TimeConstraint;
import uk.ac.horizon.ug.lobby.server.ServerProtocol;
import uk.ac.horizon.ug.lobby.user.UserGameTemplateServlet;
import uk.me.jstott.jcoord.LatLng;

/** 
 * Get Game (templates) info, for public browsing
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class JoinGameInstanceServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(JoinGameInstanceServlet.class.getName());

	private GameInstance getGameInstance(HttpServletRequest req, EntityManager em) throws RequestException {
		String id = HttpUtils.getIdFromPath(req);
		
		Key key = KeyFactory.stringToKey(id);
		GameInstance gt = em.find(GameInstance.class, key);
        if (gt==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameInstance "+id+" not found");
        
        return gt;
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		GameIndex gindex = sc.getGameIndex();

		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			GameJoinRequest gjreq = JSONUtils.parseGameJoinRequest(json);
			// second line is digital signature (if given)
			String auth = br.readLine();
			
			logger.info("GameJoinRequest "+gjreq);
			GameInstance gi = getGameInstance(req, em);
			
			GameJoinResponse gjresp = new GameJoinResponse();
			gjresp.setTime(System.currentTimeMillis());
			
			// validate 
			if (gjreq.getType()==null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"request type must be specified");
				return;
			}
			gjresp.setType(gjreq.getType());
			
			// authenticate client
			GameClient gc = null;
			Account account = null;
			boolean anonymous = false;
			if (gjreq.getClientId()==null) {
				// anonymous attempt
				if (!gi.isAllowAnonymousClients()) {
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game does not allow anonymous players");
					return;
				}
				if (gjreq.getGameSlotId()!=null){
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Changing an existing game slot requires a client to be identified");
					return;
				}
				// ensure possible createAnonymousClient will be atomic wrt to the next check...
				et.commit();
				et.begin();
				
				// does default client already exist?
				String clientId = gjreq.getDeviceId();
				if (clientId!=null) {
					Query q = em.createQuery("SELECT x FROM "+GameClient.class.getSimpleName()+" x WHERE x."+ID+" = :"+ID);
					q.setParameter(ID, clientId);
					List<GameClient> gcs = (List<GameClient>)q.getResultList();
					if (gcs.size()>0) {
						gc = gcs.get(0);
						if (gc.getAccountKey()!=null || gc.getSharedSecret()!=null) {
							logger.warning("Client deviceId="+gjreq.getDeviceId()+" already exists, non-anonymous");
							gc = null;
							clientId = null;
						}
						else
							logger.info("Using default anonymous client with deviceId="+gjreq.getDeviceId());
					}
				}
				if (gc==null) {
	
					// create is done in our transaction
					gc = createAnonymousClient(em, gjreq, clientId);
				}

				et.commit();
				et.begin();
				anonymous = true;
			}
			else {
				// identified client 
				String clientId = gjreq.getClientId();
				Query q = em.createQuery("SELECT x FROM "+GameClient.class.getSimpleName()+" x WHERE x."+ID+" = :"+ID);
				q.setParameter(ID, clientId);
				List<GameClient> gcs = (List<GameClient>)q.getResultList();
				if (gcs.size()==0) {
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_AUTHENTICATION_FAILED, "GameClient "+clientId+" unknown");
					return;
				}
				gc = gcs.get(0);
				if (gc.getAccountKey()!=null) {
					account = em.find(Account.class, gc.getAccountKey());
					if (account==null) {
						logger.warning("GameClient "+clientId+" found but Account missing: "+gc);
						sendError(resp, gjresp, GameJoinResponseStatus.ERROR_AUTHENTICATION_FAILED, "This clientId is not usable");
						return;						
					}
				}
				else {
					if (!gi.isAllowAnonymousClients()) {
						sendError(resp, gjresp, GameJoinResponseStatus.ERROR_USER_AUTHENTICATION_REQUIRED, "This game does not allow anonymous players");
						return;
					}
					anonymous = true;
				}
				// authenticate
				if (!authenticateRequest(line, gc, account, auth)) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication failed");
					return;					
				}
			}
			gjresp.setClientId(gc.getId());
			
			// existing game slot?
			GameInstanceSlot gs = null;
			if (gjreq.getGameSlotId()!=null) {
				if (gc==null) {
					// should already be checked
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Changing an existing game slot requires a client to be identified");
					return;
				}
				Key gskey = GameInstanceSlot.idToKey(gi.getKey(), gjreq.getGameSlotId());
				gs = em.find(GameInstanceSlot.class, gskey);
				if (gs==null) {
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_UNKNOWN_SLOT, "GameSlot "+gjreq.getGameSlotId()+" not found");
					return;
				}
				// correct client?
				if (!gs.getGameClientKey().equals(gc.getKey())) {
					// TODO another client of the same account?!
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_NOT_PERMITTED, "Game slot "+gjreq.getGameSlotId()+" is not owned by client "+gjreq.getClientId());
					return;
				}
			}
			else {
				// already got a slot for this client?
				Query q;
				q = em.createQuery("SELECT x FROM "+GameInstanceSlot.class.getSimpleName()+" x WHERE x."+GAME_INSTANCE_KEY+" = :"+GAME_INSTANCE_KEY+" AND x."+GAME_CLIENT_KEY+" = :"+GAME_CLIENT_KEY);
				q.setParameter(GAME_INSTANCE_KEY, gi.getKey());
				q.setParameter(GAME_CLIENT_KEY, gc.getKey());
				List<GameInstanceSlot> gss = (List<GameInstanceSlot>)q.getResultList();
				if (gss.size()>0) {
					gs = gss.get(0);
					logger.warning("Found existing Game slot "+gs.getKey().getName()+" for client "+gc.getId());
				}			
			}
			
			// create new game slot 
			if (gs==null) {
				// new slot
				if (gjreq.getType()==GameJoinRequestType.RELEASE) {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Release request for unspecified gameSlotId");
					return;
				}
				// new Game Slot...
				
				// check if full
				Query q;
				q = em.createQuery("SELECT x FROM "+GameInstanceSlot.class.getSimpleName()+" x WHERE x."+GAME_INSTANCE_KEY+" = :"+GAME_INSTANCE_KEY);
				q.setParameter(GAME_INSTANCE_KEY, gi.getKey());
				List<GameInstanceSlot> gss = (List<GameInstanceSlot>)q.getResultList();
				if (gss.size()!=gi.getNumSlotsAllocated()) {
					logger.warning("NumSlotsAllocated was wrong ("+gi.getNumSlotsAllocated()+" vs "+gss.size()+")");
					gi.setNumSlotsAllocated(gss.size());
				}
				boolean full = gss.size() >= gi.getMaxNumSlots();
				if (full != gi.isFull()) {
					logger.warning("Full was wrong ("+gi.isFull()+" vs "+full+")");
					gi.setFull(full);
				}
				if (full) {
					// num/full might have changed
					et.commit();
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_FULL, "This game is full");
					return;
				}
				
				// check that a valid client type exists
				List<GameClientTemplate> gcts = getGameClientTemplates(em, gjreq, gi.getGameTemplateId());
	
				if (gcts.size()==0) {
					// num/full might have changed
					et.commit();
					sendError(resp, gjresp, GameJoinResponseStatus.ERROR_UNSUPPORTED_CLIENT, "This game does not support your client");
					return;
				}
				
				GameClientTemplate gct = null;
				if (gcts.size()>1) {
					logger.warning("Found "+gcts.size()+" possible client templates - taking first");
					// TODO feedback?
				}
				gct = gcts.get(0);

				logger.info(gjreq.getType()+" using client "+gct);
				gi.setNumSlotsAllocated(gi.getNumSlotsAllocated()+1);
				if (gi.getNumSlotsAllocated() >= gi.getMaxNumSlots()) {
					logger.info("Game now full: "+gi);
					gi.setFull(full);
				}

				gs = new GameInstanceSlot();				
				gs.setKey(GameInstanceSlot.idToKey(gi.getKey(), GUIDFactory.newGUID()));
				if (account!=null)
					gs.setAccountKey(account.getKey());
				gs.setClientSharedSecret(createClientSharedSecret());
				gs.setGameClientKey(gc.getKey());
				gs.setGameInstanceKey(gi.getKey());
				gs.setGameTemplateId(gi.getGameTemplateId());
				gs.setStatus(GameInstanceSlotStatus.ALLOCATED);
				em.persist(gs);
			}
			et.commit();
			et.begin();
			
			gjresp.setGameSlotId(gs.getKey().getName());
			
			// must have gc, gi & gs by this point (and account if gc is linked to account)
			
			// do server operation / update game slot...
			switch (gjreq.getType()) {
			case PLAY:
				// attempt to (re)register with server				
				handleClientPlayRequest(gjreq, gi, gs, gjresp, gc, account, em);
				break;
			case RELEASE:
				// update gi
				gi.setNumSlotsAllocated(gi.getNumSlotsAllocated()-1);
				gi.setFull(gi.getNumSlotsAllocated() >= gi.getMaxNumSlots());
				em.merge(gi);
				em.remove(gs);
				logger.info("Released "+gs);
				gjresp.setStatus(GameJoinResponseStatus.OK);	
				gjresp.setMessage("Release game slot");
				break;
			case RESERVE:
				// no-op (if we have got this far)
				gjresp.setStatus(GameJoinResponseStatus.OK);
				gjresp.setPlayTime(gi.getStartTime());
				gjresp.setMessage("Game slot is reserved");
				break;
			}
			et.commit();

			// response
			JSONUtils.sendGameJoinResponse(resp, gjresp);
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		finally {	
			if(et.isActive())
				et.rollback();
			em.close();
		}
	}
	/** default poll delay (30s?!) */
	public static final int DEFAULT_POLL_INTERVAL_MS = 30000;
	/** client request to play (authenticated, etc.).
	 * @return true if handled ok; false if error send */
	private void handleClientPlayRequest(GameJoinRequest gjreq,
			GameInstance gi, GameInstanceSlot gs, GameJoinResponse gjresp, GameClient gc, Account account, EntityManager em) {

		long now = System.currentTimeMillis();
		// is game instance nominally available?
		switch(gi.getNominalStatus()) {
		case PLANNED:
		case POSSIBLE:
		case TEMPORARILY_UNAVAILABLE:
			if (now < gi.getStartTime()) {
				// try later...
				gjresp.setPlayTime(gi.getStartTime());
				setError(gjresp, GameJoinResponseStatus.TRY_LATER, "Please try again at the game start time");
			} else {
				logger.warning("GameInstance "+gi+" should have started but is still "+gi.getNominalStatus());
				setTryLater(gjresp);
			}
			return;
		case AVAILABLE:
			// cont...
			break;
		case CANCELLED:
			setError(gjresp, GameJoinResponseStatus.ERROR_CANCELLED, "Sorry - the game has been cancelled");
			return;
		case ENDED:
			setError(gjresp, GameJoinResponseStatus.ERROR_ENDED, "Sorry - the game has now ended");
			if (now < gi.getEndTime())
				logger.warning("Game has ended before advertised end time: "+gi);
			return;
		}
		// pretend it has ended?
		if (now > gi.getEndTime()) {
			logger.warning("Sending ended response for nominally active game after endTime: "+gi);
			setError(gjresp, GameJoinResponseStatus.ERROR_ENDED, "Sorry - the game has now ended");
			return;
		}
		// try to join! Fail -> Temp. Unavail.
		if (gi.getGameServerId()==null) {
			logger.warning("Game server not configured for "+gi);
			setTryLater(gjresp);
		}
		GameServer server = em.find(GameServer.class, gi.getGameServerId());
		if (server==null) {
			logger.warning("Could not find GameServer "+gi.getGameServerId()+" for "+gi);
			setTryLater(gjresp);
			return;
		}
		if (server.getTargetStatus()!=GameServerStatus.UP) {
			logger.warning("GameServer "+server.getTitle()+" is not intended to be up ("+server.getTargetStatus()+" for "+gi);
			setTryLater(gjresp);			
		}
		
		ServerProtocol serverProtocol = server.getType().serverProtocol();
		serverProtocol.handlePlayRequest(gjreq, gjresp, gi, gs, server, gc, account, em);
		return;			
	}

	public static void setTryLater(GameJoinResponse gjresp) {
		gjresp.setPlayTime(System.currentTimeMillis()+DEFAULT_POLL_INTERVAL_MS);
		setError(gjresp, GameJoinResponseStatus.TRY_LATER, "The game is not available right now - please try again in a minute");
	}

	/** send 'error' in our GameJoinResponse (i.e. not HTTP error) */
	private void sendError(HttpServletResponse resp, GameJoinResponse gjresp, GameJoinResponseStatus errorStatus) throws IOException {
		// TODO user friendly
		sendError(resp, gjresp, errorStatus, errorStatus.name());
	}
	/** send 'error' in our GameJoinResponse (i.e. not HTTP error) */
	private void sendError(HttpServletResponse resp, GameJoinResponse gjresp, GameJoinResponseStatus errorStatus, String message) throws IOException {
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

	private static char nibble(int i) {
		if (i<10)
			return (char)('0'+i);
		else 
			return (char)('a'+i-10);
	}

	private boolean authenticateRequest(String line, GameClient gc,
			Account account, String auth) {
		logger.warning("Authenticate "+line+" with "+auth+" for "+gc+" ("+account+")");
		// TODO
		return true;
	}

	private GameClient createAnonymousClient(EntityManager em, GameJoinRequest gjreq, String clientId) {
		GameClient gc = new GameClient();
		if (clientId==null)
			clientId = GUIDFactory.newGUID();
		gc.setId(clientId);
		gc.setKey(GameClient.idToKey(null, clientId));
		if (gjreq.getClientType()!=null)
			gc.setClientType(gjreq.getClientType());
		if (gjreq.getMajorVersion()!=null)
			gc.setMajorVersion(gjreq.getMajorVersion());
		if (gjreq.getMinorVersion()!=null)
			gc.setMinorVersion(gjreq.getMinorVersion());
		if (gjreq.getUpdateVersion()!=null)
			gc.setUpdateVersion(gjreq.getUpdateVersion());
		EntityTransaction et = em.getTransaction();
		em.persist(gc);
		logger.info("Created anonymous client "+clientId);
		return gc;
	}

	private List<GameClientTemplate> getGameClientTemplates(EntityManager em,
			GameJoinRequest gjreq, String gameTemplateId) {
		return QueryGameTemplateServlet.getGameClientTemplates(em, gjreq.getClientTitle(), gjreq.getClientType(), gameTemplateId, gjreq.getMajorVersion(), gjreq.getMinorVersion(), gjreq.getUpdateVersion());
	}
}
