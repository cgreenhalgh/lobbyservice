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
import uk.ac.horizon.ug.lobby.browser.JoinUtils.JoinAuthInfo;
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

		// parse request
		String line = null;
		String auth = null;
		GameJoinRequest gjreq = null;
		try {
			BufferedReader br = req.getReader();
			line = br.readLine();
			JSONObject json = new JSONObject(line);
			gjreq = JSONUtils.parseGameJoinRequest(json);
			// second line is digital signature (if given)
			auth = br.readLine();
			
			logger.info("GameJoinRequest "+gjreq);
			// check type supported...
			if (gjreq.getType()!=GameJoinRequestType.PLAY && gjreq.getType()!=GameJoinRequestType.RELEASE && gjreq.getType()!=GameJoinRequestType.RESERVE) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request type must be PLAY/RELEASE/RESERVER ("+gjreq.getType()+")");
				return;
			}
		}
		catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;			
		}
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		try {
			et.begin();
			GameInstance gi = getGameInstance(req, em);
			et.rollback();
			et.begin();
			
			GameJoinResponse gjresp = new GameJoinResponse();
			gjresp.setTime(System.currentTimeMillis());
			
			gjresp.setType(gjreq.getType());
			
			// authenticate client
			et.rollback();
			// own em/etc.
			JoinUtils.ClientInfo clientInfo = new JoinUtils.ClientInfo(gjreq.getClientType(), gjreq.getMajorVersion(), gjreq.getMinorVersion(), gjreq.getUpdateVersion());
			JoinUtils.JoinAuthInfo jai = JoinUtils.authenticate(gjreq.getClientId(), gjreq.getDeviceId(), clientInfo, gi.isAllowAnonymousClients(), line, auth);
			if (jai.anonymous && gjreq.getGameSlotId()!=null){
				throw new JoinException(GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Changing an existing game slot requires a client to be identified");
			}
			gjresp.setClientId(jai.gc.getId());
			et.begin();
			
			handleJoinRequestInternal(sc, em, et, gjreq, gjresp, jai, gi);

			// write final response
			JSONUtils.sendGameJoinResponse(resp, gjresp);
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JoinException e) {
			GameJoinResponse gjresp = new GameJoinResponse();
			gjresp.setTime(System.currentTimeMillis());
			gjresp.setType(gjreq.getType());
			gjresp.setStatus(e.getStatus());
			gjresp.setMessage(e.getMessage());
			logger.warning(e.toString());
			JSONUtils.sendGameJoinResponse(resp, gjresp);
			return;
		}
		finally {	
			if(et.isActive())
				et.rollback();
			em.close();
		}
	}
	/** common code for Join (above) and NEW_INSTANCE 
	 * @throws JoinException 
	 * @throws RequestException */
	static void handleJoinRequestInternal(ServerConfiguration sc, EntityManager em,
			EntityTransaction et, GameJoinRequest gjreq,
			GameJoinResponse gjresp, JoinAuthInfo jai, GameInstance gi) throws IOException, JoinException, RequestException {
		// TODO Auto-generated method stub
		GameClient gc = jai.gc;
		Account account = jai.account;
		boolean anonymous = jai.anonymous;

		gjresp.setClientId(gc.getId());
		// existing game slot?
		GameInstanceSlot gs = null;
		if (gjreq.getGameSlotId()!=null) {
			if (gc==null) {
				// should already be checked
				throw new JoinException(GameJoinResponseStatus.ERROR_CLIENT_AUTHENTICATION_REQUIRED, "Changing an existing game slot requires a client to be identified");
			}
			Key gskey = GameInstanceSlot.idToKey(gi.getKey(), gjreq.getGameSlotId());
			gs = em.find(GameInstanceSlot.class, gskey);
			if (gs==null) {
				throw new JoinException(GameJoinResponseStatus.ERROR_UNKNOWN_SLOT, "GameSlot "+gjreq.getGameSlotId()+" not found");
			}
			// correct client?
			if (!gs.getGameClientKey().equals(gc.getKey())) {
				// TODO another client of the same account?!
				throw new JoinException(GameJoinResponseStatus.ERROR_NOT_PERMITTED, "Game slot "+gjreq.getGameSlotId()+" is not owned by client "+gjreq.getClientId());
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
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "Release request for unspecified gameSlotId");
			}
			// new Game Slot...
			
			// check if full
			// TODO re-work as this isn't really consistent anyway
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
				throw new JoinException(GameJoinResponseStatus.ERROR_FULL, "This game is full");
			}
			
			// check that a valid client type exists
			List<GameClientTemplate> gcts = JoinUtils.getGameClientTemplates(em, gjreq, gi.getGameTemplateId());

			if (gcts.size()==0) {
				// num/full might have changed
				et.commit();
				throw new JoinException(GameJoinResponseStatus.ERROR_UNSUPPORTED_CLIENT, "This game does not support your client");
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
			gs.setClientSharedSecret(JoinUtils.createClientSharedSecret());
			gs.setGameClientKey(gc.getKey());
			gs.setGameInstanceKey(gi.getKey());
			gs.setGameTemplateId(gi.getGameTemplateId());
			gs.setStatus(GameInstanceSlotStatus.ALLOCATED);
			if (gjreq.getNickname()!=null) 
				gs.setNickname(gjreq.getNickname());
			else if (gc.getNickname()!=null)
				gs.setNickname(gc.getNickname());
			else if (account!=null && account.getNickname()!=null)
				gs.setNickname(account.getNickname());
			else
				gs.setNickname("Anonymous");
			em.persist(gs);
		} else if (gjreq.getNickname()!=null && !gjreq.getNickname().equals(gs.getNickname())){
			logger.info("Change GameSlot nickname "+gs.getNickname()+" -> "+gjreq.getNickname());
			gs.setNickname(gjreq.getNickname());
			em.merge(gs);
		}
		et.commit();
		et.begin();
		
		gjresp.setGameSlotId(gs.getKey().getName());
		gjresp.setNickname(gs.getNickname());
		
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
	}

	/** client request to play (authenticated, etc.).
	 * @return true if handled ok; false if error send */
	private static void handleClientPlayRequest(GameJoinRequest gjreq,
			GameInstance gi, GameInstanceSlot gs, GameJoinResponse gjresp, GameClient gc, Account account, EntityManager em) {

		long now = System.currentTimeMillis();
		// is game instance nominally available?
		switch(gi.getNominalStatus()) {
		case PLANNED:
		//case POSSIBLE: // not supported at the moment (waiting for dynamic game support)
		case TEMPORARILY_UNAVAILABLE:
			if (now < gi.getStartTime()) {
				// try later...
				gjresp.setPlayTime(gi.getStartTime());
				JoinUtils.setError(gjresp, GameJoinResponseStatus.TRY_LATER, "Please try again at the game start time");
			} else {
				logger.warning("GameInstance "+gi+" should have started but is still "+gi.getNominalStatus());
				JoinUtils.setTryLater(gjresp);
			}
			return;
		case AVAILABLE:
			// cont...
			break;
		case CANCELLED:
			JoinUtils.setError(gjresp, GameJoinResponseStatus.ERROR_CANCELLED, "Sorry - the game has been cancelled");
			return;
		case ENDED:
			JoinUtils.setError(gjresp, GameJoinResponseStatus.ERROR_ENDED, "Sorry - the game has now ended");
			if (now < gi.getEndTime())
				logger.warning("Game has ended before advertised end time: "+gi);
			return;
		}
		// pretend it has ended?
		if (now > gi.getEndTime()) {
			logger.warning("Sending ended response for nominally active game after endTime: "+gi);
			JoinUtils.setError(gjresp, GameJoinResponseStatus.ERROR_ENDED, "Sorry - the game has now ended");
			return;
		}
		// try to join! Fail -> Temp. Unavail.
		if (gi.getGameServerId()==null) {
			logger.warning("Game server not configured for "+gi);
			JoinUtils.setTryLater(gjresp);
		}
		GameServer server = em.find(GameServer.class, gi.getGameServerId());
		if (server==null) {
			logger.warning("Could not find GameServer "+gi.getGameServerId()+" for "+gi);
			JoinUtils.setTryLater(gjresp);
			return;
		}
		if (server.getTargetStatus()!=GameServerStatus.UP) {
			logger.warning("GameServer "+server.getTitle()+" is not intended to be up ("+server.getTargetStatus()+" for "+gi);
			JoinUtils.setTryLater(gjresp);			
		}
		
		ServerProtocol serverProtocol = server.getType().serverProtocol();
		serverProtocol.handlePlayRequest(gjreq, gjresp, gi, gs, server, gc, account, em);
		return;			
	}
}
