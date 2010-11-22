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
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import uk.ac.horizon.ug.lobby.browser.JoinGameInstanceServlet;
import uk.ac.horizon.ug.lobby.browser.JoinUtils;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.model.GameClientKnownType;
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
	/** do post of xml, return Connection */
	public static Document doPost(String surl, Document doc) throws IOException {
		return doPost(surl, doc, true);
	}
	/** do post of xml, return Connection */
	public static Document doPost(String surl, Document doc, boolean readResponse) throws IOException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			URL url = new  URL(surl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if (doc!=null)
				conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(DEFAULT_TIMEOUT_MS);
			conn.setReadTimeout(DEFAULT_TIMEOUT_MS);
			conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			logger.info("Send request "+doc+" to "+url);
			if (doc!=null) {
				OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
				// TODO (standard) Xstream doesn't work on GAE :-(
				Transformer dt = TransformerFactory.newInstance().newTransformer();
				dt.transform(new DOMSource(doc), new StreamResult(osw));
				osw.close();
			}
			int status = conn.getResponseCode();
			if (status!=HttpServletResponse.SC_OK) 
				throw new IOException("HTTP response "+status+": "+conn.getResponseMessage());

			if (readResponse) {
				InputStream is = conn.getInputStream();
				doc = db.parse(is);
				is.close();
				logger.info("Received response "+doc+" from "+url);
			}
			else {
				InputStream is = conn.getInputStream();
				is.close();
				doc = null;
			}
			return doc;
		}		
		catch (IOException e) {
			throw e;
		} catch (TransformerConfigurationException e) {
			logger.warning("doPost: "+e);
			throw new IOException(e.toString());
		} catch (TransformerFactoryConfigurationError e) {
			logger.warning("doPost: "+e);
			throw new IOException(e.toString());
		} catch (TransformerException e) {
			logger.warning("doPost: "+e);
			throw new IOException(e.toString());
		} catch (SAXException e) {
			logger.warning("doPost: "+e);
			throw new IOException(e.toString());
		} catch (ParserConfigurationException e) {
			logger.warning("doPost: "+e);
			throw new IOException(e.toString());
		}
	}
	@Override
	public void handlePlayRequest(GameJoinRequest gjreq,
			GameJoinResponse gjresp, GameInstance gi, GameInstanceSlot gs,
			GameServer server, GameClient gc, Account account) {
		// Register client with the server.
		// Post 
		//  <login>
		//   <clientId>...</clientId>
		//   <conversationId>...</conversationId>
		//   <playerName>...</playerName>
		//   <clientVersion>1</clientVersion>
		//   <clientType>AndroidDevclient</clientType>
		//   <gameTag>...</gameTag>
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
			gs.setClientSharedSecret(JoinUtils.createClientSharedSecret(20*4));
			addElement(doc, "conversationId", gs.getClientSharedSecret());
			gjresp.getPlayData().put("conversationId", gs.getClientSharedSecret());
			
			if (!GameClientKnownType.Android.toString().equals(gc.getClientType())) {
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
			addElement(doc, "gameTag", getGameTag(gi));

			if (gi.getBaseUrl()!=null && !gi.getBaseUrl().equals(server.getBaseUrl()))
				logger.warning("GameInstance baseUrl does not match server baseUrl ("+gi.getBaseUrl()+" vs "+server.getBaseUrl()+") for "+gi);
			String url = server.getBaseUrl()+"/rpc/login";
			doc = doPost(url, doc);

			String replyStatus = getElement(doc, "status");
			if (!"OK".equals(replyStatus)) {
				if ("FAILED".equals(replyStatus) || "GAME_NOT_FOUND".equals(replyStatus)) {
					logger.warning("Retryable error logging into "+url+": reply");
					// worth retrying
					JoinUtils.setTryLater(gjresp);
					return;
				}
				logger.warning("Terminal error logging into "+url+": reply");
				gjresp.setStatus(GameJoinResponseStatus.ERROR_INTERNAL);
				gjresp.setMessage("Unable to join game ("+getElement(doc, "message")+")");
				return;
			}
			gjresp.getPlayData().put("gameId", getElement(doc, "gameId"));
			gjresp.getPlayData().put("gameStatus", getElement(doc, "gameStatus"));
		} catch (IOException e) {
			logger.log(Level.WARNING, "Problem doing login with URL based on "+server.getBaseUrl(), e);
			JoinUtils.setTryLater(gjresp);
			return;
		} catch (ParserConfigurationException e) {
			logger.log(Level.WARNING, "Problem with XML parser", e);
			JoinUtils.setError(gjresp, GameJoinResponseStatus.ERROR_INTERNAL, "Problem with ExplodingPlaces protocol");
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
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		// TODO audit
		doPost(server.getBaseUrl()+"/orchestration/play.html?gameID="+getGameId(gi), null, false);
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceEnd(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceEnd(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		// TODO audit
		doPost(server.getBaseUrl()+"/orchestration/stop.html?gameID="+getGameId(gi), null, false);
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceEndingFromActive(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceEndingFromActive(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		// TODO audit
		doPost(server.getBaseUrl()+"/orchestration/finish.html?gameID="+getGameId(gi), null, false);
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#validate(uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer)
	 */
	@Override
	public void validate(GameInstanceFactory factory, GameServer server)
			throws ConfigurationException {
		getServerConfig(factory);
	}
	public static final String CONTENT_GROUP = "contentGroup";
	private JSONObject getServerConfig(GameInstanceFactory factory) throws ConfigurationException {
		if (factory.getServerConfigJson()==null) {
			throw new ConfigurationException("serverConfigJson undefined");
		}
		try {
			JSONObject o = new JSONObject(factory.getServerConfigJson());
			if (!o.has(CONTENT_GROUP))
				throw new ConfigurationException("Config must include "+CONTENT_GROUP+" property");
			JSONObject contentGroup = o.getJSONObject(CONTENT_GROUP);
			return o;
		}
		catch (JSONException e) {
			throw new ConfigurationException(e.toString()+" for "+factory.getServerConfigJson());
		}
		
	}
	private JSONObject getContentGroupConfig(JSONObject config) throws ConfigurationException {
		try {
			return config.getJSONObject(CONTENT_GROUP);
		} catch (JSONException e) {
			throw new ConfigurationException("Config must include "+CONTENT_GROUP+" property with object value");
		}
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstancePreparingFromPlanned(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstancePreparingFromPlanned(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		// identify appropriate ContentGroup

		// serverConfigJson must include 'contentGroup':{...}
		// where inner properties filter ContentGroups, e.g. name, location, version
		JSONObject contentGroupConfig = getContentGroupConfig(getServerConfig(factory));
		String getContentGroupsUrl = server.getBaseUrl()+"/orchestration/content_group_list";
		// e.g.  
		// <array size="2" elementjavatype="java.lang.Object">
		//   <item>
		//     <ContentGroup package="uk.ac.horizon.ug.exploding.db">
		//       <ID>CG500</ID> 
		//       <name>gameState.xml</name> 
		//       <version>1.0</version> 
		//       <location>Woolwich</location> 
		//       <startYear>1900</startYear> 
		//       <endYear>2020</endYear> 
		//     </ContentGroup>
		//   </item>
		//   ...
		Document doc = doPost(getContentGroupsUrl, null);
		String contentGroupId = null;
		
		Element rootEl = doc.getDocumentElement();
		NodeList items= rootEl.getElementsByTagName("item");
		nextitem:
		for (int ii=0; ii<items.getLength(); ii++) {
			Element itemEl = (Element)items.item(ii);
			NodeList cgs = rootEl.getElementsByTagName("ContentGroup");
			for (int cgi=0; cgi<cgs.getLength(); cgi++) {
				Element cgEl = (Element)cgs.item(cgi);
				NodeList cns = cgEl.getChildNodes();
				String id = null;
				for (int cni=0; cni<cns.getLength(); cni++) {
					Node cn = cns.item(cni);
					if (cn instanceof Element) {
						Element cnEl = (Element)cn;
						String name = cnEl.getTagName();
						String value = cnEl.getTextContent();
						if (contentGroupConfig.has(name)) {
							try {
								if (!value.equals(contentGroupConfig.get(name)))
									// mis-match
									continue nextitem;
							}
							catch (JSONException je) {/*shouldn't happen*/}
						}
						if (name.equals("ID"))
							id = value;
					}
				}
				// satisfied any constraints
				if (id!=null) {
					contentGroupId = id;
					break nextitem;
				}
			}				
		}
		
		if (contentGroupId==null) 
			throw new ConfigurationException("Server has no ContentGroup matching "+contentGroupConfig.toString());
		
		// generate and store Game Tag (for use with login)
		String name = gi.getTitle()+"/"+(new Date(gi.getStartTime()));
		String gameTag = gi.getTitle()+"/"+(new Date(gi.getStartTime()))+"/"+UUID.randomUUID().toString();
		
		// create game using orchestration form
		// POST with url-encoded contentGroupID, name and tag to orchestration/create.html
		String createUrl = server.getBaseUrl()+"/orchestration/lobby_create?"+
			"contentGroupID="+URLEncoder.encode(contentGroupId, "UTF-8")+
			"&name="+URLEncoder.encode(name, "UTF-8")+
			"&tag="+URLEncoder.encode(gameTag, "UTF-8");
		
		// TODO audit
		doc = doPost(createUrl, null);
		// returns something like:
		// <?xml version="1.0"?>
		// <Game package="uk.ac.horizon.ug.exploding.db">
		//   <ID>GA514</ID>
		//   <contentGroupID>CG500</contentGroupID>
		//   <name>name</name>
		//   <tag>tag</tag>
		//   <timeCreated>1283957181008</timeCreated>
		//   <gameTimeID>GT514</gameTimeID>
		//   <state>NOT_STARTED</state>
		//  </Game>
		String gameId = getElement(doc, "ID");
		if (gameId==null)
			throw new IOException("No ID in return from lobby_create");

		// store generated Game ID
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			JSONObject config = new JSONObject();
			config.put(GAME_ID, gameId);
			config.put(GAME_TAG, gameTag);
			GameInstance ngi = em.find(GameInstance.class, gi.getKey());
			ngi.setServerConfigJson(config.toString());
			em.merge(ngi);
			et.commit();
			
			// don't fiddle the cached value
			
		} catch (Exception e) {
			throw new IOException("Problem saving gameId ("+gameId+"): "+e);
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
	}
	public static final String GAME_ID = "gameId";
	public static final String GAME_TAG = "gameTag";
	private String getGameId(GameInstance gi) throws IOException {
		if (gi.getServerConfigJson()==null) {
			throw new IOException("instance serverConfigJson undefined");
		}
		try {
			JSONObject o = new JSONObject(gi.getServerConfigJson());
			return o.getString(GAME_ID);
		}
		catch (JSONException e) {
			throw new IOException(e.toString()+" for "+gi.getServerConfigJson());
		}
	}
	private String getGameTag(GameInstance gi) throws IOException {
		if (gi.getServerConfigJson()==null) {
			throw new IOException("instance serverConfigJson undefined");
		}
		try {
			JSONObject o = new JSONObject(gi.getServerConfigJson());
			return o.getString(GAME_TAG);
		}
		catch (JSONException e) {
			throw new IOException(e.toString()+" for "+gi.getServerConfigJson());
		}
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.lobby.server.ServerProtocol#handleGameInstanceReadyFromPreparing(uk.ac.horizon.ug.lobby.model.GameInstance, uk.ac.horizon.ug.lobby.model.GameInstanceFactory, uk.ac.horizon.ug.lobby.model.GameServer, javax.persistence.EntityManager)
	 */
	@Override
	public void handleGameInstanceReadyFromPreparing(GameInstance gi,
			GameInstanceFactory factory, GameServer server) {
		// no-op
	}

}
