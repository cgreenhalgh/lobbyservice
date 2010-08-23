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
package uk.ac.horizon.ug.lobby.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

import uk.ac.horizon.ug.lobby.browser.JoinGameInstanceServlet;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientType;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceSlot;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequest;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponse;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponseStatus;

/**
 * @author cmg
 *
 */
public class ExplodingPlacesServerProtocol implements ServerProtocol {
	static Logger logger = Logger.getLogger(ExplodingPlacesServerProtocol.class.getName());

	static class LoginMessage {
		/** client ID (IMEI) */
		String clientId;
		/** conversation ID */
		String conversationId;
		/** player name */
		String playerName;
		/** client version */
		int clientVersion;
		/** client type */
		String clientType;
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "LoginMessage [clientId=" + clientId + ", clientType="
					+ clientType + ", clientVersion=" + clientVersion
					+ ", conversationId=" + conversationId + ", playerName="
					+ playerName + "]";
		}
	}
	/** status enum */
	public static enum Status {
		NOT_DONE,
		FAILED,
		OK, 
		OLD_CLIENT_VERSION, BAD_CLIENT_VERSION, 
		UNSUPPORTED_CLIENT_TYPE,
		GAME_NOT_FOUND, 
		FORBIDDEN,
		SERVER_CLOSED
	};
	static class LoginReplyMessage {
		/** game ID (server internal) - for info */
		private String gameId;
		/** game status */
		private String gameStatus;
		/** status response */
		private Status status = Status.NOT_DONE;
		/** message (to user) */
		private String message;
		/** detail message */
		private String detail;
		/** cons */
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "LoginReplyMessage [detail=" + detail + ", gameId=" + gameId
					+ ", gameStatus=" + gameStatus + ", message=" + message
					+ ", status=" + status + "]";
		}
		
	}
	static final int DEFAULT_TIMEOUT_MS = 30000;
	@Override
	public void handlePlayRequest(GameJoinRequest gjreq,
			GameJoinResponse gjresp, GameInstance gi, GameInstanceSlot gs,
			GameServer server, GameClient gc, Account account, EntityManager em) {
		// Register client with the server.
		// Post 
		//  <login>
		//   <clientId>...</clientId>
		//   <conversationId>...</conversationId>
		//   <playerName>...</playerName>
		//   <clientVersion>1</clientVersion>
		//   <clientType>AndroidDevclient</clientType>
		//  </login>
		
		// to baseUrl/rpc/login.
		if (gjresp.getPlayData()==null)
			gjresp.setPlayData(new HashMap<String,Object>());
		try {
			LoginMessage login = new LoginMessage();
			// clientId is meant to be durable across retries/restarts - our clientId is probably OK
			login.clientId = gc.getId();
			// conversationId is limited to 20 chars. 
			// each log should be a new conversationId.
			gs.setClientSharedSecret(JoinGameInstanceServlet.createClientSharedSecret(20*4));
			login.conversationId = gs.getClientSharedSecret();
			gjresp.getPlayData().put("conversationId", login.conversationId);
			
			if (gc.getClientType()!=GameClientType.ANDROID) {
				logger.warning("ExplodingPlaces only supports ANDROID clientType");
				gjresp.setStatus(GameJoinResponseStatus.ERROR_UNSUPPORTED_CLIENT);
				gjresp.setMessage("Sorry - this game only supports Android client(s)");
				return;
			}
			login.clientType= "AndroidDevclient";
			login.clientVersion= 1;
			// TODO playerName
			login.playerName= "lobbyPlayer";

			if (gi.getBaseUrl()!=null && !gi.getBaseUrl().equals(server.getBaseUrl()))
				logger.warning("GameInstance baseUrl does not match server baseUrl ("+gi.getBaseUrl()+" vs "+server.getBaseUrl()+") for "+gi);
			URL url = new  URL(server.getBaseUrl()+"/rpc/login");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(DEFAULT_TIMEOUT_MS);
			conn.setReadTimeout(DEFAULT_TIMEOUT_MS);
			conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			logger.info("Send request "+login+" to "+url);
			OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			// TODO (standard) Xstream doesn't work on GAE :-(
			XStream xs = new XStream(new PureJavaReflectionProvider());
			xs.alias("login", LoginMessage.class);
			xs.toXML(login, osw);
			osw.close();
			int status = conn.getResponseCode();
			if (status!=HttpServletResponse.SC_OK) 
				throw new IOException("HTTP response "+status+": "+conn.getResponseMessage());
			InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "UTF-8");
			xs.alias("reply", LoginReplyMessage.class);
			LoginReplyMessage reply = (LoginReplyMessage)xs.fromXML(isr);
			isr.close();
			logger.info("Received response "+reply+" from "+url);
			if (reply.status!=Status.OK) {
				if (reply.status==Status.FAILED || reply.status==Status.GAME_NOT_FOUND) {
					logger.warning("Retryable error logging into "+url+": reply");
					// worth retrying
					JoinGameInstanceServlet.setTryLater(gjresp);
					return;
				}
				logger.warning("Terminal error logging into "+url+": reply");
				gjresp.setStatus(GameJoinResponseStatus.ERROR_INTERNAL);
				gjresp.setMessage("Unable to join game ("+reply.message+")");
				return;
			}
			gjresp.getPlayData().put("gameId", reply.gameId);
			gjresp.getPlayData().put("gameStatus", reply.gameStatus);
		} catch (MalformedURLException e) {
			logger.warning("Problem with request URL based on "+server.getBaseUrl()+": "+e);
			JoinGameInstanceServlet.setTryLater(gjresp);
			return;
		} catch (IOException e) {
			logger.log(Level.WARNING, "Problem doing login with URL based on "+server.getBaseUrl(), e);
			JoinGameInstanceServlet.setTryLater(gjresp);
		}

		// generate client play URL. (Note conversationId is a required parameter)
		// baseUrl/messages
		gjresp.setPlayUrl(server.getBaseUrl()+"/rpc/messages");

		gjresp.setStatus(GameJoinResponseStatus.OK);
	}

}
