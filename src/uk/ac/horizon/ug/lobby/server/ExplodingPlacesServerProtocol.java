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
import java.io.InputStream;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import uk.ac.horizon.ug.lobby.browser.JoinGameInstanceServlet;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientType;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
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
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			doc.appendChild(doc.createElement("login"));
			// clientId is meant to be durable across retries/restarts - our clientId is probably OK
			addElement(doc, "clientId", gc.getId());
			// conversationId is limited to 20 chars. 
			// each log should be a new conversationId.
			gs.setClientSharedSecret(JoinGameInstanceServlet.createClientSharedSecret(20*4));
			addElement(doc, "conversationId", gs.getClientSharedSecret());
			gjresp.getPlayData().put("conversationId", gs.getClientSharedSecret());
			
			if (gc.getClientType()!=GameClientType.ANDROID) {
				logger.warning("ExplodingPlaces only supports ANDROID clientType");
				gjresp.setStatus(GameJoinResponseStatus.ERROR_UNSUPPORTED_CLIENT);
				gjresp.setMessage("Sorry - this game only supports Android client(s)");
				return;
			}
			addElement(doc, "clientType", "AndroidDevclient");
			addElement(doc, "clientVersion", "1");
			String nickname = gs.getNickname();
			if (nickname==null) 		
				nickname = "Anonymous";
			gjresp.setNickname(nickname);
			addElement(doc, "playerName", nickname);

			// TODO include game tag
			
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
			logger.info("Send request "+doc+" to "+url);
			OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			// TODO (standard) Xstream doesn't work on GAE :-(
			Transformer dt = TransformerFactory.newInstance().newTransformer();
			dt.transform(new DOMSource(doc), new StreamResult(osw));
			osw.close();
			int status = conn.getResponseCode();
			if (status!=HttpServletResponse.SC_OK) 
				throw new IOException("HTTP response "+status+": "+conn.getResponseMessage());
			InputStream is = conn.getInputStream();
			doc = db.parse(is);
			is.close();
			logger.info("Received response "+doc+" from "+url);
			String replyStatus = getElement(doc, "status");
			if (!"OK".equals(replyStatus)) {
				if ("FAILED".equals(replyStatus) || "GAME_NOT_FOUND".equals(replyStatus)) {
					logger.warning("Retryable error logging into "+url+": reply");
					// worth retrying
					JoinGameInstanceServlet.setTryLater(gjresp);
					return;
				}
				logger.warning("Terminal error logging into "+url+": reply");
				gjresp.setStatus(GameJoinResponseStatus.ERROR_INTERNAL);
				gjresp.setMessage("Unable to join game ("+getElement(doc, "message")+")");
				return;
			}
			gjresp.getPlayData().put("gameId", getElement(doc, "gameId"));
			gjresp.getPlayData().put("gameStatus", getElement(doc, "gameStatus"));
		} catch (MalformedURLException e) {
			logger.warning("Problem with request URL based on "+server.getBaseUrl()+": "+e);
			JoinGameInstanceServlet.setTryLater(gjresp);
			return;
		} catch (IOException e) {
			logger.log(Level.WARNING, "Problem doing login with URL based on "+server.getBaseUrl(), e);
			JoinGameInstanceServlet.setTryLater(gjresp);
			return;
		} catch (ParserConfigurationException e) {
			logger.log(Level.WARNING, "Problem with XML parser", e);
			JoinGameInstanceServlet.setError(gjresp, GameJoinResponseStatus.ERROR_INTERNAL, "Problem with ExplodingPlaces protocol");
			return;
		} catch (TransformerConfigurationException e) {
			logger.log(Level.WARNING, "Problem with XML transformer", e);
			JoinGameInstanceServlet.setError(gjresp, GameJoinResponseStatus.ERROR_INTERNAL, "Problem with ExplodingPlaces protocol");
			return;
		} catch (TransformerFactoryConfigurationError e) {
			logger.log(Level.WARNING, "Problem with XML transformer", e);
			JoinGameInstanceServlet.setError(gjresp, GameJoinResponseStatus.ERROR_INTERNAL, "Problem with ExplodingPlaces protocol");
			return;
		} catch (TransformerException e) {
			logger.log(Level.WARNING, "Problem with transforming request", e);
			JoinGameInstanceServlet.setTryLater(gjresp);
			return;
		} catch (SAXException e) {
			logger.log(Level.WARNING, "Problem with parsing request", e);
			JoinGameInstanceServlet.setTryLater(gjresp);
			return;
		}

		// generate client play URL. (Note conversationId is a required parameter)
		// baseUrl/messages
		gjresp.setPlayUrl(server.getBaseUrl()+"/rpc/");

		gjresp.setStatus(GameJoinResponseStatus.OK);
	}
	private String getElement(Document doc, String tag) {
		NodeList els = doc.getDocumentElement().getElementsByTagName(tag);
		if (els.getLength()==0)
			return null;
		return ((Element)els.item(0)).getTextContent();
	}
	private void addElement(Document doc, String tag, String value) {
		Element el = doc.createElement(tag);
		doc.getDocumentElement().appendChild(el);
		el.appendChild(doc.createTextNode(value));		
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceActiveFromReady(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceActiveFromReady(GameInstance gi,
			GameInstanceFactory factory, GameServer server, EntityManager em) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Unimplemented");
		
		// TODO start game instance
		
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceEnd(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceEnd(GameInstance gi,
			GameInstanceFactory factory, GameServer server, EntityManager em) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Unimplemented");
		
		// TODO end/finish game instance
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceEndingFromActive(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceEndingFromActive(GameInstance gi,
			GameInstanceFactory factory, GameServer server, EntityManager em) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Unimplemented");
		
		// TODO ending game instance
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstancePreparingFromPlanned(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstancePreparingFromPlanned(GameInstance gi,
			GameInstanceFactory factory, GameServer server, EntityManager em) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Unimplemented");
		// TODO actually create a game instance
		// TODO requires identifying appropriate ContentGroup
		// TODO requires generating and storing Game Tag (for use with login)
		// TODO requires storing of generated Game ID
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceReadyFromPreparing(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceReadyFromPreparing(GameInstance gi,
			GameInstanceFactory factory, GameServer server, EntityManager em) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Unimplemented");
		
		// TODO no-op
	}

}
