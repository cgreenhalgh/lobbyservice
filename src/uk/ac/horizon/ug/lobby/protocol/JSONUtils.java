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
package uk.ac.horizon.ug.lobby.protocol;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.appengine.api.datastore.KeyFactory;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameClientType;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
import uk.ac.horizon.ug.lobby.model.GameServerType;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.user.UserGameTemplateServlet;

/** JSON marshall/unmarshall utils
 * 
 * @author cmg
 *
 */
public class JSONUtils implements Constants {
	static Logger logger = Logger.getLogger(JSONUtils.class.getName());
	/** write Account object for user 
	 * @throws JSONException */
	public static void writeUserAccount(JSONWriter jw, Account account) throws JSONException {
		jw.object();
		jw.key("userId");
		jw.value(account.getUserId());
		jw.key("nickname");
		jw.value(account.getNickname());
		jw.key("gameTemplateQuota");
		jw.value(account.getGameTemplateQuota());
		jw.endObject();
	}
	/** write GameTemplateInfo
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplateInfo gameTemplateInfo) throws JSONException {
		writeGameTemplate(jw, gameTemplateInfo.getGameTemplate(), gameTemplateInfo.getGameClientTemplates(), gameTemplateInfo.getQueryUrl(), gameTemplateInfo.getGameInstance(), gameTemplateInfo.getJoinUrl());
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplate gameTemplate) throws JSONException {
		writeGameTemplate(jw, gameTemplate, null, null, null, null);
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplate gameTemplate, List<GameClientTemplate> gameClientTemplates, String queryUrl, GameInstance gameInstance, String joinUrl) throws JSONException {
		jw.object();
		if (gameTemplate.getId()!=null) {
			jw.key(ID);
			jw.value(gameTemplate.getId());
		}
		if (gameTemplate.getTitle()!=null) {
			jw.key(TITLE);
			jw.value(gameTemplate.getTitle());
		}
		if (gameTemplate.getDescription()!=null) {
			jw.key(DESCRIPTION);
			jw.value(gameTemplate.getDescription());
		}
		if (gameTemplate.getLanguage()!=null) {
			jw.key(LANGUAGE);
			jw.value(gameTemplate.getLanguage());
		}
		if (gameTemplate.getLink()!=null) {
			jw.key(LINK);
			jw.value(gameTemplate.getLink());
		}
		if (gameTemplate.getImageUrl()!=null) {
			jw.key(IMAGE_URL);
			jw.value(gameTemplate.getImageUrl());
		}
		// game instance visibility over-rides us if given
		if (gameTemplate.getVisibility()!=null && (gameInstance==null || gameInstance.getVisibility()==null)) {
			jw.key(VISIBILITY);
			jw.value(gameTemplate.getVisibility().toString());
		}
		if (gameClientTemplates!=null) {
			jw.key(CLIENT_TEMPLATES);
			jw.array();
			for (GameClientTemplate gameClientTemplate : gameClientTemplates) {
				jw.object();
				if (gameClientTemplate.getTitle()!=null) {
					jw.key(TITLE);
					jw.value(gameClientTemplate.getTitle());
				}
				if (gameClientTemplate.getClientType()!=null) {
					jw.key(CLIENT_TYPE);
					jw.value(gameClientTemplate.getClientType().toString());
				}
				jw.key(MIN_MAJOR_VERSION);
				jw.value(gameClientTemplate.getMinMajorVersion());
				jw.key(MIN_MINOR_VERSION);
				jw.value(gameClientTemplate.getMinMinorVersion());
				jw.key(MIN_UPDATE_VERSION);
				jw.value(gameClientTemplate.getMinUpdateVersion());
				jw.key(LOCATION_SPECIFIC);
				jw.value(gameClientTemplate.isLocationSpecific());
				if (gameClientTemplate.getApplicationLaunchId()!=null) {
					jw.key(APPLICATION_LAUNCH_ID);
					jw.value(gameClientTemplate.getApplicationLaunchId());
				}
				if (gameClientTemplate.getApplicationMarketId()!=null) {
					jw.key(APPLICATION_MARKET_ID);
					jw.value(gameClientTemplate.getApplicationMarketId());
				}
				jw.endObject();
			}	
			jw.endArray();
		}
		if (queryUrl!=null) {
			jw.key(QUERY_URL);
			jw.value(queryUrl);
		}
		if (gameInstance!=null) {
			writeGameInstancePublicFields(jw, gameInstance, true);
		}
		if (joinUrl!=null) {
			jw.key(JOIN_URL);
			jw.value(joinUrl);
		}
		jw.endObject();
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplates(JSONWriter jw, List<GameTemplate> gameTemplates) throws JSONException {
		jw.array();
		for (GameTemplate gameTemplate : gameTemplates) {
			writeGameTemplate(jw, gameTemplate);
		}
		jw.endArray();
	}
	/** write GameTemplates details
	 * @throws JSONException */
	public static void writeGameTemplateInfos(JSONWriter jw, List<GameTemplateInfo> gameTemplates) throws JSONException {
		jw.array();
		for (GameTemplateInfo gameTemplate : gameTemplates) {
			writeGameTemplate(jw, gameTemplate);
		}
		jw.endArray();
	}
	/** get output stream writer for JSON (setting encoding and mime type) 
	 * @throws IOException 
	 * @throws IOException */
	public static BufferedWriter getResponseWriter(HttpServletResponse resp) throws IOException {
		resp.setCharacterEncoding(ENCODING);
		resp.setContentType(JSON_MIME_TYPE);		
		return new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), ENCODING));
	}
	/** set Account as response 
	 * @throws IOException */
	public static void sendAccount(HttpServletResponse resp, Account account) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeUserAccount(jw, account);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** set Account as response 
	 * @throws IOException */
	public static void sendGameTemplate(HttpServletResponse resp, GameTemplateInfo gameTemplateInfo) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameTemplate(jw, gameTemplateInfo);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** parse JSON Object to GameTemplateInfo, i.e. GameTemplate with optional GameClientTemplates 
	 * @throws JSONException */
	public static GameTemplateInfo parseGameTemplateInfo(JSONObject json) throws JSONException {
		GameTemplateInfo gti = new GameTemplateInfo();
		GameTemplate gt = new GameTemplate();
		gti.setGameTemplate(gt);
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(TITLE))
				gt.setTitle(json.getString(TITLE));
			else if (key.equals(DESCRIPTION))
				gt.setDescription(json.getString(DESCRIPTION));
			else if (key.equals(LANGUAGE))
				gt.setLanguage(json.getString(LANGUAGE));
			else if (key.equals(LINK))
				gt.setLink(json.getString(LINK));
			else if (key.equals(IMAGE_URL))
				gt.setImageUrl(json.getString(IMAGE_URL));
			else if (key.equals(ID))
				gt.setId(json.getString(ID));
			else if (key.equals(VISIBILITY))
				gt.setVisibility(GameTemplateVisibility.valueOf(json.getString(VISIBILITY)));
			else if (key.equals(CLIENT_TEMPLATES)) {
				JSONArray jarray = json.getJSONArray(CLIENT_TEMPLATES);
				List<GameClientTemplate> gcts = new LinkedList<GameClientTemplate>();
				gti.setGameClientTemplates(gcts);
				for (int i=0; i<jarray.length(); i++) {
					JSONObject jobj = jarray.getJSONObject(i);
					gcts.add(parseGameClientTemplate(jobj));
				}
			}
			else
				throw new JSONException("Unsupported key '"+key+"' in GameTemplate: "+json);
		}
		return gti;
	}
	/** parse JSON Object to GameTemplateInfo, i.e. GameTemplate with optional GameClientTemplates 
	 * @throws JSONException */
	public static GameClientTemplate parseGameClientTemplate(JSONObject json) throws JSONException {
		GameClientTemplate gct = new GameClientTemplate();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
//			logger.info("GCT: "+key+"="+json.getObject(key));
			if (key.equals(TITLE))
				gct.setTitle(json.getString(TITLE));
			else if (key.equals(CLIENT_TYPE))
				gct.setClientType(GameClientType.valueOf(json.getString(CLIENT_TYPE)));
			else if (key.equals(MIN_MAJOR_VERSION))
				gct.setMinMajorVersion(json.getInt(MIN_MAJOR_VERSION));
			else if (key.equals(MIN_MINOR_VERSION))
				gct.setMinMinorVersion(json.getInt(MIN_MINOR_VERSION));
			else if (key.equals(MIN_UPDATE_VERSION))
				gct.setMinUpdateVersion(json.getInt(MIN_UPDATE_VERSION));
			else if (key.equals(APPLICATION_LAUNCH_ID))
				gct.setApplicationLaunchId(json.getString(APPLICATION_LAUNCH_ID));
			else if (key.equals(LOCATION_SPECIFIC))
				gct.setLocationSpecific(json.getBoolean(LOCATION_SPECIFIC));
			else if (key.equals(APPLICATION_MARKET_ID)) {
				gct.setApplicationMarketId(json.getString(APPLICATION_MARKET_ID));
				logger.info("ApplicationMarketId="+gct.getApplicationMarketId());
			}
			else 
				throw new JSONException("Unsupported key '"+key+"' in GameClientTemplate: "+json);
		}
		return gct;
	}
	/** write GameServer object for user 
	 * @throws JSONException */
	public static void writeGameServers(JSONWriter jw, List<GameServer> gss) throws JSONException {
		jw.array();
		for (GameServer gs : gss) {
			writeGameServer(jw, gs);
		}
		jw.endArray();
	}
	/** write GameServer object for user 
	 * @throws JSONException */
	public static void writeGameServer(JSONWriter jw, GameServer gs) throws JSONException {
		jw.object();
		if (gs.getBaseUrl()!=null) {
			jw.key(BASE_URL);
			jw.value(gs.getBaseUrl());
		}
		if (gs.getGameTemplateId()!=null) {
			jw.key(GAME_TEMPLATE_ID);
			jw.value(gs.getGameTemplateId());
		}
		if (gs.getKey()!=null) {
			jw.key(KEY);
			jw.value(KeyFactory.keyToString(gs.getKey()));
		}
		if (gs.getLastKnownStatus()!=null) {
			jw.key(LAST_KNOWN_STATUS);
			jw.value(gs.getLastKnownStatus().toString());
		}
		if (gs.getTitle()!=null) {
			jw.key(TITLE);
			jw.value(gs.getTitle().toString());
		}
		if (gs.getLastKnownStatusTime()!=0) {
			jw.key(LAST_KNOWN_STATUS_TIME);
			jw.value(gs.getLastKnownStatusTime());
		}
		if (gs.getLobbySharedSecret()!=null) {
			jw.key(LOBBY_SHARED_SECRET);
			jw.value(gs.getLobbySharedSecret());
		}
		if (gs.getTargetStatus()!=null) {
			jw.key(TARGET_STATUS);
			jw.value(gs.getTargetStatus().toString());
		}
		if (gs.getType()!=null) {
			jw.key(TYPE);
			jw.value(gs.getType().toString());
		}
		jw.endObject();
	}
	/** parse JSON Object to GameTemplateInfo, i.e. GameTemplate with optional GameClientTemplates 
	 * @throws JSONException */
	public static GameServer parseGameServer(JSONObject json) throws JSONException {
		GameServer gs = new GameServer();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(BASE_URL))
				gs.setBaseUrl(json.getString(BASE_URL));
			else if (key.equals(GAME_TEMPLATE_ID))
				gs.setGameTemplateId(json.getString(GAME_TEMPLATE_ID));
			else if (key.equals(KEY))
				gs.setKey(KeyFactory.stringToKey(json.getString(KEY)));
			else if (key.equals(LAST_KNOWN_STATUS))
				gs.setLastKnownStatus(GameServerStatus.valueOf(json.getString(LAST_KNOWN_STATUS)));
			else if (key.equals(LAST_KNOWN_STATUS_TIME))
				gs.setLastKnownStatusTime(json.getInt(LAST_KNOWN_STATUS_TIME));
			else if (key.equals(LOBBY_SHARED_SECRET))
				gs.setLobbySharedSecret(json.getString(LOBBY_SHARED_SECRET));
			else if (key.equals(TARGET_STATUS))
				gs.setTargetStatus(GameServerStatus.valueOf(json.getString(TARGET_STATUS)));
			else if (key.equals(TYPE))
				gs.setType(GameServerType.valueOf(json.getString(TYPE)));
			else if (key.equals(TITLE))
				gs.setTitle(json.getString(TITLE));
			else
				throw new JSONException("Unsupported key '"+key+"' in GameServer: "+json);
		}
		return gs;
	}
	/** set GameServer as response 
	 * @throws IOException */
	public static void sendGameServer(HttpServletResponse resp, GameServer gs) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameServer(jw, gs);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** write GameInstances for user 
	 * @throws JSONException */
	public static void writeGameInstances(JSONWriter jw, List<GameInstance> gss) throws JSONException {
		jw.array();
		for (GameInstance gs : gss) {
			writeGameInstance(jw, gs);
		}
		jw.endArray();
	}
	/** write GameInstance object for user 
	 * @throws JSONException */
	public static void writeGameInstance(JSONWriter jw, GameInstance gs) throws JSONException {
		writeGameInstance(jw, gs, null, null);
	}
	/** write GameInstance object for user 
	 * @throws JSONException */
	public static void writeGameInstance(JSONWriter jw, GameInstance gs, GameTemplate gameTemplate, GameServer gameServer) throws JSONException {
		jw.object();
		if (gs.getBaseUrl()!=null) {
			jw.key(BASE_URL);
			jw.value(gs.getBaseUrl());
		}
		if (gs.getGameServerId()!=null) {
			jw.key(GAME_SERVER_ID);
			jw.value(KeyFactory.keyToString(gs.getGameServerId()));
		}
		if (gs.getGameTemplateId()!=null) {
			jw.key(GAME_TEMPLATE_ID);
			jw.value(gs.getGameTemplateId());
		}
		if (gs.getKey()!=null) {
			jw.key(KEY);
			jw.value(KeyFactory.keyToString(gs.getKey()));
		}
		if (gs.getStatus()!=null) {
			jw.key(STATUS);
			jw.value(gs.getStatus().toString());
		}

		writeGameInstancePublicFields(jw, gs, false);

		if (gameTemplate!=null) {
			jw.key(GAME_TEMPLATE);
			writeGameTemplate(jw, gameTemplate);
		}
		if (gameServer!=null) {
			jw.key(GAME_SERVER);
			writeGameServer(jw, gameServer);
		}


		jw.endObject();
	}
	private static void writeGameInstancePublicFields(JSONWriter jw,
			GameInstance gs, boolean escapeTitle) throws JSONException {
		// TODO Auto-generated method stub
		jw.key(ALLOW_ANONYMOUS_CLIENTS);
		jw.value(gs.isAllowAnonymousClients());
		jw.key(END_TIME);
		jw.value(gs.getEndTime());
		jw.key(FULL);
		jw.value(gs.isFull());
		jw.key(LATITUDE_E6);
		jw.value(gs.getLatitudeE6());
		jw.key(LONGITUDE_E6);
		jw.value(gs.getLongitudeE6());
		jw.key(MAX_NUM_SLOTS);
		jw.value(gs.getMaxNumSlots());
		if (gs.getNominalStatus()!=null) {
			jw.key(NOMINAL_STATUS);
			jw.value(gs.getNominalStatus().toString());
		}
		jw.key(NUM_SLOTS_ALLOCATED);
		jw.value(gs.getNumSlotsAllocated());
		jw.key(RADIUS_METRES);
		jw.value(gs.getRadiusMetres());
		jw.key(START_TIME);
		jw.value(gs.getStartTime());
		if (gs.getTitle()!=null) {
			jw.key(escapeTitle ? SUBTITLE : TITLE);
			jw.value(gs.getTitle());
		}
		jw.key(VISIBILITY);
		jw.value(gs.getVisibility().toString());
	}
	/** parse JSON Object to GameInstance
	 * @throws JSONException */
	public static GameInstance parseGameInstance(JSONObject json) throws JSONException {
		GameInstance gs = new GameInstance();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(ALLOW_ANONYMOUS_CLIENTS))
				gs.setAllowAnonymousClients(json.getBoolean(key));
			else if (key.equals(BASE_URL))
				gs.setBaseUrl(json.getString(key));
			else if (key.equals(END_TIME))
				gs.setEndTime(json.getLong(key));
			else if (key.equals(GAME_SERVER_ID))
				gs.setGameServerId(KeyFactory.stringToKey(json.getString(key)));
			else if (key.equals(GAME_TEMPLATE_ID))
				gs.setGameTemplateId(json.getString(key));
			else if (key.equals(KEY))
				gs.setKey(KeyFactory.stringToKey(json.getString(key)));
			else if (key.equals(LATITUDE_E6))
				gs.setLatitudeE6(json.getInt(key));
			else if (key.equals(LONGITUDE_E6))
				gs.setLongitudeE6(json.getInt(key));
			else if (key.equals(MAX_NUM_SLOTS))
				gs.setMaxNumSlots(json.getInt(key));
			else if (key.equals(NOMINAL_STATUS))
				gs.setNominalStatus(GameInstanceNominalStatus.valueOf(json.getString(key)));
			else if (key.equals(RADIUS_METRES))
				gs.setRadiusMetres(json.getDouble(key));
			else if (key.equals(START_TIME))
				gs.setStartTime(json.getLong(key));
			else if (key.equals(STATUS))
				gs.setStatus(GameInstanceStatus.valueOf(json.getString(key)));
			else if (key.equals(TITLE))
				gs.setTitle(json.getString(key));
			else if (key.equals(VISIBILITY))
				gs.setVisibility(GameTemplateVisibility.valueOf(json.getString(key)));
			else
				throw new JSONException("Unsupported key '"+key+"' in GameInstance: "+json);
		}
		return gs;
	}
	/** set GameInstance as response 
	 * @throws IOException */
	public static void sendGameInstance(HttpServletResponse resp, GameInstance gs, GameTemplate gameTemplate, GameServer gameServer) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameInstance(jw, gs);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** write GameIndex object for user 
	 * @throws JSONException */
	public static void writeGameIndex(JSONWriter jw, GameIndex gs) throws JSONException {
		writeGameIndex(jw, gs, null);
	}
	/** write GameIndex object for user 
	 * @throws JSONException */
	public static void writeGameIndex(JSONWriter jw, GameIndex gs, ServerConfiguration sc) throws JSONException {
		jw.object();
		if (gs.getDescription()!=null) {
			jw.key(DESCRIPTION);
			jw.value(gs.getDescription());
		}
		if (gs.getDocs()!=null) {
			jw.key(DOCS);
			jw.value(gs.getDocs());
		}
		if (gs.getGenerator()!=null) {
			jw.key(GENERATOR);
			jw.value(gs.getGenerator());
		}
		if (gs.getImageUrl()!=null) {
			jw.key(IMAGE_URL);
			jw.value(gs.getImageUrl());
		}
		if (gs.getLanguage()!=null) {
			jw.key(LANGUAGE);
			jw.value(gs.getLanguage());
		}
		if (gs.getLastBuildDate()!=0) {
			jw.key(LAST_BUILD_DATE);
			jw.value(gs.getLastBuildDate());
		}
		if (gs.getLink()!=null) {
			jw.key(LINK);
			jw.value(gs.getLink());
		}
		if (gs.getTitle()!=null) {
			jw.key(TITLE);
			jw.value(gs.getTitle());
		}
		if (gs.getTtlMinutes()!=0) {
			jw.key(TTL_MINUTES);
			jw.value(gs.getTtlMinutes());
		}
		if (gs.getVersion()!=0) {
			jw.key(VERSION);
			jw.value(gs.getVersion());
		}
		if (gs.getItems()!=null) {
			jw.key(ITEMS);
			jw.array();
			for (GameTemplateInfo gti : gs.getItems()) {
				writeGameTemplate(jw, gti);
			}
			jw.endArray();
		}
		if (sc!=null) {
			// config special case
			if (sc.getBaseUrl()!=null) {
				jw.key(BASE_URL);
				jw.value(sc.getBaseUrl());
			}
		}
		jw.endObject();
	}
	/** set GameIndex as response 
	 * @throws IOException */
	public static void sendGameIndex(HttpServletResponse resp, GameIndex gi) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameIndex(jw, gi);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** set GameIndex as response 
	 * @throws IOException */
	public static void sendServerConfiguration(HttpServletResponse resp, ServerConfiguration sc) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameIndex(jw, sc.getGameIndex(), sc);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** parse JSON Object to GameInstance
	 * @throws JSONException */
	public static TimeConstraint parseTimeConstraint(JSONObject json) throws JSONException {
		TimeConstraint o = new TimeConstraint();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(INCLUDE_STARTED))
				o.setIncludeStarted(json.getBoolean(key));
			else if (key.equals(LIMIT_END_TIME))
				o.setLimitEndTime(json.getBoolean(key));
			else if (key.equals(MAX_DURATION_MS))
				o.setMaxDurationMs(json.getLong(key));
			else if (key.equals(MAX_TIME))
				o.setMaxTime(json.getLong(key));
			else if (key.equals(MIN_DURATION_MS))
				o.setMinDurationMs(json.getLong(key));
			else if (key.equals(MIN_TIME))
				o.setMinTime(json.getLong(key));
			else
				throw new JSONException("Unsupported key '"+key+"' in TimeConstraint: "+json);
		}
		return o;
	}
	/** parse JSON Object to GameInstance
	 * @throws JSONException */
	public static LocationConstraint parseLocationConstraint(JSONObject json) throws JSONException {
		LocationConstraint o = new LocationConstraint();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(LATITUDE_E6))
				o.setLatitudeE6(json.getInt(key));
			else if (key.equals(LONGITUDE_E6))
				o.setLongitudeE6(json.getInt(key));
			else if (key.equals(RADIUS_METRES))
				o.setRadiusMetres((float)json.getDouble(key));
			else if (key.equals(TYPE))
				o.setType(LocationConstraintType.valueOf(json.getString(key)));
			else
				throw new JSONException("Unsupported key '"+key+"' in LocationConstraint: "+json);
		}
		return o;
	}
	/** parse JSON Object to GameInstance
	 * @throws JSONException */
	public static GameQuery parseGameQuery(JSONObject json) throws JSONException {
		GameQuery o = new GameQuery();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(CLIENT_TITLE))
				o.setClientTitle(json.getString(key));
			else if (key.equals(CLIENT_TYPE))
				o.setClientType(GameClientType.valueOf(json.getString(key)));
			else if (key.equals(GAME_TEMPLATE_ID))
				o.setGameTemplateId(json.getString(key));
			else if (key.equals(LATITUDE_E6))
				o.setLatitudeE6(json.getInt(key));
			else if (key.equals(LOCATION_CONSTRAINT))
				o.setLocationConstraint(parseLocationConstraint(json.getJSONObject(key)));
			else if (key.equals(LONGITUDE_E6))
				o.setLongitudeE6(json.getInt(key));
			else if (key.equals(MAJOR_VERSION))
				o.setMajorVersion(json.getInt(key));
			else if (key.equals(MINOR_VERSION))
				o.setMinorVersion(json.getInt(key));
			else if (key.equals(TIME_CONSTRAINT))
				o.setTimeConstraint(parseTimeConstraint(json.getJSONObject(key)));
			else if (key.equals(UPDATE_VERSION))
				o.setUpdateVersion(json.getInt(key));
			else if (key.equals(VERSION))
				o.setVersion(json.getInt(key));
			else
				throw new JSONException("Unsupported key '"+key+"' in GameQuery: "+json);
		}
		return o;
	}
	/** parse JSON Object to GameJoinRequest
	 * @throws JSONException */
	public static GameJoinRequest parseGameJoinRequest(JSONObject json) throws JSONException {
		GameJoinRequest o = new GameJoinRequest();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(CLIENT_TITLE))
				o.setClientTitle(json.getString(key));
			else if (key.equals(CLIENT_ID))
				o.setClientId(json.getString(key));
			else if (key.equals(CLIENT_TYPE))
				o.setClientType(GameClientType.valueOf(json.getString(key)));
			else if (key.equals(DEVICE_ID))
				o.setDeviceId(json.getString(key));
			else if (key.equals(GAME_SLOT_ID))
				o.setGameSlotId(json.getString(key));
			else if (key.equals(LATITUDE_E6))
				o.setLatitudeE6(json.getInt(key));
			else if (key.equals(LONGITUDE_E6))
				o.setLongitudeE6(json.getInt(key));
			else if (key.equals(MAJOR_VERSION))
				o.setMajorVersion(json.getInt(key));
			else if (key.equals(NICKNAME))
				o.setNickname(json.getString(key));
			else if (key.equals(MINOR_VERSION))
				o.setMinorVersion(json.getInt(key));
			else if (key.equals(SEQ_NO))
				o.setSeqNo(json.getInt(key));
			else if (key.equals(TIME))
				o.setTime(json.getLong(key));
			else if (key.equals(TYPE))
				o.setType(GameJoinRequestType.valueOf(json.getString(key)));
			else if (key.equals(UPDATE_VERSION))
				o.setUpdateVersion(json.getInt(key));
			else if (key.equals(VERSION))
				o.setVersion(json.getInt(key));
			else
				throw new JSONException("Unsupported key '"+key+"' in GameJoinRequest: "+json);
		}
		return o;
	}
	/** write GameJoinResponse object for user 
	 * @throws JSONException */
	public static void writeGameJoinResponse(JSONWriter jw, GameJoinResponse gs) throws JSONException {
		jw.object();
		if (gs.getClientId()!=null) {
			jw.key(CLIENT_ID);
			jw.value(gs.getClientId());
		}
		if (gs.getGameSlotId()!=null) {
			jw.key(GAME_SLOT_ID);
			jw.value(gs.getGameSlotId());
		}
		if (gs.getMessage()!=null) {
			jw.key(MESSAGE);
			jw.value(gs.getMessage());
		}
		if (gs.getNickname()!=null) {
			jw.key(NICKNAME);
			jw.value(gs.getNickname());
		}
		if (gs.getPlayTime()!=null) {
			jw.key(PLAY_TIME);
			jw.value(gs.getPlayTime());
		}
		if (gs.getPlayUrl()!=null) {
			jw.key(PLAY_URL);
			jw.value(gs.getPlayUrl());
		}
		if (gs.getStatus()!=null) {
			jw.key(STATUS);
			jw.value(gs.getStatus().toString());
		}
		if (gs.getTime()!=null) {
			jw.key(TIME);
			jw.value(gs.getTime());
		}
		if (gs.getType()!=null) {
			jw.key(TYPE);
			jw.value(gs.getType().toString());
		}
		
		if (gs.getPlayData()!=null) {
			jw.key(PLAY_DATA);
			jw.object();
			for (String key : gs.getPlayData().keySet()) {
				jw.key(key);
				jw.value(gs.getPlayData().get(key));
			}
			jw.endObject();
		}
		jw.endObject();
	}
	/** set GameJoinResponse as response 
	 * @throws IOException */
	public static void sendGameJoinResponse(HttpServletResponse resp, GameJoinResponse gi) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameJoinResponse(jw, gi);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
}
