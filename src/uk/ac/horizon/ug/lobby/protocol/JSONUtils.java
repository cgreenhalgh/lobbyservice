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

/** JSON marshall/unmarshall utils
 * 
 * @author cmg
 *
 */
public class JSONUtils implements Constants {
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
		writeGameTemplate(jw, gameTemplateInfo.getGameTemplate(), gameTemplateInfo.getGameClientTemplates());
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplate gameTemplate) throws JSONException {
		writeGameTemplate(jw, gameTemplate, null);
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplate gameTemplate, List<GameClientTemplate> gameClientTemplates) throws JSONException {
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
		if (gameTemplate.getVisibility()!=null) {
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
			else if (key.equals(APPLICATION_MARKET_ID))
				gct.setTitle(json.getString(APPLICATION_MARKET_ID));
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
		jw.key(END_TIME);
		jw.value(gs.getEndTime());
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
		jw.key(LATITUDE_E6);
		jw.value(gs.getLatitudeE6());
		jw.key(LONGITUDE_E6);
		jw.value(gs.getLongitudeE6());
		if (gs.getNominalStatus()!=null) {
			jw.key(NOMINAL_STATUS);
			jw.value(gs.getNominalStatus().toString());
		}
		jw.key(RADIUS_METRES);
		jw.value(gs.getRadiusMetres());
		jw.key(START_TIME);
		jw.value(gs.getStartTime());
		if (gs.getStatus()!=null) {
			jw.key(STATUS);
			jw.value(gs.getStatus().toString());
		}
		if (gs.getTitle()!=null) {
			jw.key(TITLE);
			jw.value(gs.getTitle());
		}
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
	/** parse JSON Object to GameInstance
	 * @throws JSONException */
	public static GameInstance parseGameInstance(JSONObject json) throws JSONException {
		GameInstance gs = new GameInstance();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(BASE_URL))
				gs.setBaseUrl(json.getString(BASE_URL));
			else if (key.equals(END_TIME))
				gs.setEndTime(json.getLong(END_TIME));
			else if (key.equals(GAME_SERVER_ID))
				gs.setGameServerId(KeyFactory.stringToKey(json.getString(GAME_SERVER_ID)));
			else if (key.equals(GAME_TEMPLATE_ID))
				gs.setGameTemplateId(json.getString(GAME_TEMPLATE_ID));
			else if (key.equals(KEY))
				gs.setKey(KeyFactory.stringToKey(json.getString(KEY)));
			else if (key.equals(LATITUDE_E6))
				gs.setLatitudeE6(json.getInt(LATITUDE_E6));
			else if (key.equals(LONGITUDE_E6))
				gs.setLongitudeE6(json.getInt(LONGITUDE_E6));
			else if (key.equals(NOMINAL_STATUS))
				gs.setNominalStatus(GameInstanceNominalStatus.valueOf(json.getString(NOMINAL_STATUS)));
			else if (key.equals(RADIUS_METRES))
				gs.setRadiusMetres(json.getDouble(RADIUS_METRES));
			else if (key.equals(START_TIME))
				gs.setStartTime(json.getLong(START_TIME));
			else if (key.equals(STATUS))
				gs.setStatus(GameInstanceStatus.valueOf(json.getString(STATUS)));
			else if (key.equals(TITLE))
				gs.setTitle(json.getString(TITLE));
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
		if (gs.getItems()!=null) {
			jw.key(ITEMS);
			jw.array();
			for (GameTemplateInfo gti : gs.getItems()) {
				writeGameTemplate(jw, gti);
			}
			jw.endArray();
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
}
