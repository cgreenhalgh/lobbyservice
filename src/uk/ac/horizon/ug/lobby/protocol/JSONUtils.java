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
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryLocationType;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactoryType;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
import uk.ac.horizon.ug.lobby.model.GameServerType;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateAuditRecord;
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
		writeGameTemplate(jw, gameTemplateInfo.getGameTemplate(), gameTemplateInfo.getGameClientTemplates(), gameTemplateInfo.getQueryUrl(), gameTemplateInfo.getGameInstance(), gameTemplateInfo.getJoinUrl(), gameTemplateInfo.getGameInstanceFactory(), gameTemplateInfo.getFirstStartTime(), gameTemplateInfo.getGameTimeOptions(), gameTemplateInfo.getNewInstanceUrl());
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplate gameTemplate) throws JSONException {
		writeGameTemplate(jw, gameTemplate, null, null, null, null, null, null, null, null);
	}
	/** write GameTemplate summary 
	 * @throws JSONException */
	public static void writeGameTemplate(JSONWriter jw, GameTemplate gameTemplate, List<GameClientTemplate> gameClientTemplates, String queryUrl, GameInstance gameInstance, String joinUrl, GameInstanceFactory gameInstanceFactory, Long firstStartTime, GameTimeOptions gameTimeOptions, String newInstanceUrl) throws JSONException {
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
		if (gameTemplate.getVisibility()!=null && (gameInstance==null || gameInstance.getVisibility()==null) && (gameInstanceFactory==null || gameInstanceFactory.getVisibility()==null)) {
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
		if (newInstanceUrl!=null) {
			jw.key(NEW_INSTANCE_URL);
			jw.value(newInstanceUrl);
		}
		if (gameInstance!=null) {
			writeGameInstancePublicFields(jw, gameInstance, true);
		}
		else if (gameInstanceFactory!=null) {
			writeGameInstanceFactoryPublicFields(jw, gameInstanceFactory, true);
			if (firstStartTime!=null) {
				jw.key(FIRST_START_TIME);
				jw.value(firstStartTime.longValue());
			}

		}
		if (gameTimeOptions!=null) {
			jw.key(TIME_OPTIONS);
			writeGameTimeOptions(jw, gameTimeOptions);
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
		if (gs.getCreatedTime()!=null) {
			jw.key(CREATED_TIME);
			jw.value(gs.getCreatedTime());
		}
		if (gs.getGameInstanceFactoryKey()!=null) {
			jw.key(GAME_INSTANCE_FACTORY_KEY);
			jw.value(KeyFactory.keyToString(gs.getGameInstanceFactoryKey()));
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
		if (gs.getServerConfigJson()!=null) {
			jw.key(SERVER_CONFIG_JSON);
			jw.value(gs.getServerConfigJson().toString());
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
		if (gs.getLocationName()!=null) {
			jw.key(LOCATION_NAME);
			jw.value(gs.getLocationName());
		}
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
			else if (key.equals(LOCATION_NAME))
				gs.setLocationName(json.getString(key));
			else if (key.equals(LONGITUDE_E6))
				gs.setLongitudeE6(json.getInt(key));
			else if (key.equals(MAX_NUM_SLOTS))
				gs.setMaxNumSlots(json.getInt(key));
			else if (key.equals(NOMINAL_STATUS))
				gs.setNominalStatus(GameInstanceNominalStatus.valueOf(json.getString(key)));
			else if (key.equals(RADIUS_METRES))
				gs.setRadiusMetres(json.getDouble(key));
			// not serverConfigJson?!
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
	/** write GameInstanceFactories for user 
	 * @throws JSONException */
	public static void writeGameInstanceFactories(JSONWriter jw, List<GameInstanceFactory> gss) throws JSONException {
		jw.array();
		for (GameInstanceFactory gs : gss) {
			writeGameInstanceFactory(jw, gs);
		}
		jw.endArray();
	}
	/** write GameInstanceFactory object for user 
	 * @throws JSONException */
	public static void writeGameInstanceFactory(JSONWriter jw, GameInstanceFactory gs) throws JSONException {
		writeGameInstanceFactory(jw, gs, null, null);
	}
	/** write GameInstanceFactory object for user 
	 * @throws JSONException */
	public static void writeGameInstanceFactory(JSONWriter jw, GameInstanceFactory gs, GameTemplate gameTemplate, GameServer gameServer) throws JSONException {
		jw.object();
		if (gs.getGameServerId()!=null) {
			jw.key(GAME_SERVER_ID);
			jw.value(KeyFactory.keyToString(gs.getGameServerId()));
		}
		if (gs.getGameTemplateId()!=null) {
			jw.key(GAME_TEMPLATE_ID);
			jw.value(gs.getGameTemplateId());
		}
		jw.key(INSTANCE_CREATE_TIME_WINDOW_MS);
		jw.value(gs.getInstanceCreateTimeWindowMs());
		if (gs.getInstanceTitle()!=null) {
			jw.key(INSTANCE_TITLE);
			jw.value(gs.getInstanceTitle());
		}
		if (gs.getInstanceVisibility()!=null) {
			jw.key(INSTANCE_VISIBILITY);
			jw.value(gs.getInstanceVisibility().toString());
		}
		if (gs.getKey()!=null) {
			jw.key(KEY);
			jw.value(KeyFactory.keyToString(gs.getKey()));
		}
		jw.key(LAST_INSTANCE_CHECK_TIME);
		jw.value(gs.getLastInstanceCheckTime());
		jw.key(LAST_INSTANCE_START_TIME);
		jw.value(gs.getLastInstanceStartTime());
		jw.key(NEW_INSTANCE_TOKENS);
		jw.value(gs.getNewInstanceTokens());
		jw.key(NEW_INSTANCE_TOKENS_MAX);
		jw.value(gs.getNewInstanceTokensMax());
		jw.key(NEW_INSTANCE_TOKENS_PER_HOUR);
		jw.value(gs.getNewInstanceTokensPerHour());
		if (gs.getServerConfigJson()!=null) {
			jw.key(SERVER_CONFIG_JSON);
			jw.value(gs.getServerConfigJson());
		}
		jw.key(SERVER_CREATE_TIME_OFFSET_MS);
		jw.value(gs.getServerCreateTimeOffsetMs());
		jw.key(SERVER_END_TIME_OFFSET_MS);
		jw.value(gs.getServerEndTimeOffsetMs());
		jw.key(SERVER_ENDING_TIME_OFFSET_MS);
		jw.value(gs.getServerEndingTimeOffsetMs());
		jw.key(SERVER_START_TIME_OFFSET_MS);
		jw.value(gs.getServerStartTimeOffsetMs());
		if (gs.getStatus()!=null) {
			jw.key(STATUS);
			jw.value(gs.getStatus().toString());
		}

		writeGameInstanceFactoryPublicFields(jw, gs, false);

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
	// visible to query results
	private static void writeGameInstanceFactoryPublicFields(JSONWriter jw,
			GameInstanceFactory gs, boolean escapeTitle) throws JSONException {
		// TODO Auto-generated method stub
		jw.key(ALLOW_ANONYMOUS_CLIENTS);
		jw.value(gs.isAllowAnonymousClients());
		jw.key(ALLOW_PRIVATE_INSTANCES);
		jw.value(gs.isAllowPrivateInstances());
		jw.key(CREATE_FOR_ANONYMOUS_CLIENT);
		jw.value(gs.isCreateForAnonymousClient());
		if (gs.getType()!=null) {
			jw.key(TYPE);
			jw.value(gs.getType().toString());
		}
		jw.key(DURATION_MS);
		jw.value(gs.getDurationMs());
		jw.key(LATITUDE_E6);
		jw.value(gs.getLatitudeE6());
		if (gs.getLocationType()!=null) {
			jw.key(LOCATION_TYPE);
			jw.value(gs.getLocationType().toString());
		}
		if (gs.getLocationName()!=null) {
			jw.key(LOCATION_NAME);
			jw.value(gs.getLocationName());
		}
		jw.key(LONGITUDE_E6);
		jw.value(gs.getLongitudeE6());
		jw.key(MAX_NUM_SLOTS);
		jw.value(gs.getMaxNumSlots());
		jw.key(MAX_TIME);
		jw.value(gs.getMaxTime());
		jw.key(MIN_TIME);
		jw.value(gs.getMinTime());
		jw.key(RADIUS_METRES);
		jw.value(gs.getRadiusMetres());
		if (gs.getStartTimeCron()!=null) {
			jw.key(START_TIME_CRON);
			jw.value(gs.getStartTimeCron());
		}
		if (gs.getStartTimeOptionsJson()!=null) {
			jw.key(START_TIME_OPTIONS_JSON);
			jw.value(gs.getStartTimeOptionsJson());
		}
		//jw.key(STATUS);
		//jw.value(gs.getStatus().toString());
		if (gs.getTitle()!=null) {
			jw.key(escapeTitle ? SUBTITLE : TITLE);
			jw.value(gs.getTitle());
		}
		jw.key(VISIBILITY);
		jw.value(gs.getVisibility().toString());
	}
	/** set GameInstanceFactory as response 
	 * @throws IOException */
	public static void sendGameInstanceFactory(HttpServletResponse resp, GameInstanceFactory gs, GameTemplate gameTemplate, GameServer gameServer) throws IOException {
		Writer w = JSONUtils.getResponseWriter(resp);
		JSONWriter jw = new JSONWriter(w);
		try {
			JSONUtils.writeGameInstanceFactory(jw, gs);	
		} catch (JSONException je) {
			throw new IOException(je);
		}
		w.close();
	}
	/** parse JSON Object to GameInstanceFactory
	 * @throws JSONException */
	public static GameInstanceFactory parseGameInstanceFactory(JSONObject json) throws JSONException {
		GameInstanceFactory gs = new GameInstanceFactory();
		Iterator keys = json.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (key.equals(ALLOW_ANONYMOUS_CLIENTS))
				gs.setAllowAnonymousClients(json.getBoolean(key));
			else if (key.equals(ALLOW_PRIVATE_INSTANCES))
				gs.setAllowPrivateInstances(json.getBoolean(key));
			else if (key.equals(CREATE_FOR_ANONYMOUS_CLIENT))
				gs.setCreateForAnonymousClient(json.getBoolean(key));
			else if (key.equals(DURATION_MS))
				gs.setDurationMs(json.getLong(key));
			else if (key.equals(GAME_SERVER_ID))
				gs.setGameServerId(KeyFactory.stringToKey(json.getString(key)));
			else if (key.equals(GAME_TEMPLATE_ID))
				gs.setGameTemplateId(json.getString(key));
			else if (key.equals(INSTANCE_CREATE_TIME_WINDOW_MS))
				gs.setInstanceCreateTimeWindowMs(json.getLong(key));
			else if (key.equals(INSTANCE_TITLE))
				gs.setInstanceTitle(json.getString(key));
			else if (key.equals(INSTANCE_VISIBILITY))
				gs.setInstanceVisibility(GameTemplateVisibility.valueOf(json.getString(key)));
			else if (key.equals(KEY))
				gs.setKey(KeyFactory.stringToKey(json.getString(key)));
			// not lastInstanceCheckTime
			// not lastInstanceStartTime
			else if (key.equals(LATITUDE_E6))
				gs.setLatitudeE6(json.getInt(key));
			else if (key.equals(LOCATION_NAME))
				gs.setLocationName(json.getString(key));
			else if (key.equals(LOCATION_TYPE))
				gs.setLocationType(GameInstanceFactoryLocationType.valueOf(json.getString(key)));
			else if (key.equals(LONGITUDE_E6))
				gs.setLongitudeE6(json.getInt(key));
			// not newInstanceTokens
			else if (key.equals(NEW_INSTANCE_TOKENS_MAX))
				gs.setNewInstanceTokensMax(json.getInt(key));
			else if (key.equals(NEW_INSTANCE_TOKENS_PER_HOUR))
				gs.setNewInstanceTokensPerHour(json.getInt(key));
			else if (key.equals(MAX_NUM_SLOTS))
				gs.setMaxNumSlots(json.getInt(key));
			else if (key.equals(MAX_TIME))
				gs.setMaxTime(json.getLong(key));
			else if (key.equals(MIN_TIME))
				gs.setMinTime(json.getLong(key));
			else if (key.equals(RADIUS_METRES))
				gs.setRadiusMetres(json.getDouble(key));
			else if (key.equals(SERVER_CONFIG_JSON))
				gs.setServerConfigJson(json.getString(key));
			else if (key.equals(SERVER_CREATE_TIME_OFFSET_MS))
				gs.setServerCreateTimeOffsetMs(json.getLong(key));
			else if (key.equals(SERVER_ENDING_TIME_OFFSET_MS))
				gs.setServerEndingTimeOffsetMs(json.getLong(key));
			else if (key.equals(SERVER_END_TIME_OFFSET_MS))
				gs.setServerEndTimeOffsetMs(json.getLong(key));
			else if (key.equals(SERVER_START_TIME_OFFSET_MS))
				gs.setServerStartTimeOffsetMs(json.getLong(key));
			else if (key.equals(START_TIME_CRON))
				gs.setStartTimeCron(json.getString(key));
			// not startTimeOptionsJson
			else if (key.equals(STATUS))
				gs.setStatus(GameInstanceFactoryStatus.valueOf(json.getString(key)));
			else if (key.equals(TITLE))
				gs.setTitle(json.getString(key));
			else if (key.equals(TYPE))
				gs.setType(GameInstanceFactoryType.valueOf(json.getString(key)));
			else if (key.equals(VISIBILITY))
				gs.setVisibility(GameTemplateVisibility.valueOf(json.getString(key)));
			else
				throw new JSONException("Unsupported key '"+key+"' in GameInstance: "+json);
		}
		return gs;
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
			jw.key(MAX_NEW_INSTANCE_TOKENS_MAX);
			jw.value(sc.getMaxNewInstanceTokensMax());
			jw.key(MAX_NEW_INSTANCE_TOKENS_PER_HOUR);
			jw.value(sc.getMaxNewInstanceTokensPerHour());
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
			else if (key.equals(NEW_INSTANCE_START_TIME))
				o.setNewInstanceStartTime(json.getLong(key));
			else if (key.equals(NEW_INSTANCE_VISIBILITY))
				o.setNewInstanceVisibility(GameTemplateVisibility.valueOf(json.getString(key)));
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
		if (gs.getJoinUrl()!=null) {
			jw.key(JOIN_URL);
			jw.value(gs.getJoinUrl());
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
	/** write GameJoinResponse object for user 
	 * @throws JSONException */
	public static void writeGameTimeOptions(JSONWriter jw, GameTimeOptions o) throws JSONException {
		jw.object();
		if (o.getDayOfMonth()!=null) {
			jw.key(DAY_OF_MONTH);
			writeGameTimeOption(jw, o.getDayOfMonth());
		}
		if (o.getDayOfWeek()!=null) {
			jw.key(DAY_OF_WEEK);
			writeGameTimeOption(jw, o.getDayOfWeek());
		}
		if (o.getHour()!=null) {
			jw.key(HOUR);
			writeGameTimeOption(jw, o.getHour());
		}
		jw.endObject();
		if (o.getMinute()!=null) {
			jw.key(MINUTE);
			writeGameTimeOption(jw, o.getMinute());
		}
		if (o.getMonth()!=null) {
			jw.key(MONTH);
			writeGameTimeOption(jw, o.getMonth());
		}
		if (o.getSecond()!=null) {
			jw.key(SECOND);
			writeGameTimeOption(jw, o.getSecond());
		}
		if (o.getYear()!=null) {
			jw.key(YEAR);
			writeGameTimeOption(jw, o.getYear());
		}
	}
	/**
	 * @param jw
	 * @param year
	 * @throws JSONException 
	 */
	private static void writeGameTimeOption(JSONWriter jw, GameTimeOption o) throws JSONException {
		jw.object();
		jw.key(INITIAL_VALUE);
		jw.value(o.getInitialValue());
		if (o.getOptions()!=null) {
			jw.key(OPTIONS);
			jw.array();
			for (int i=0; i<o.getOptions().length; i++) 
				jw.value(o.getOptions()[i]);
			jw.endArray();
		}
		jw.endObject();
	}
	/** write GameTemplateAuditRecord object for user 
	 * @throws JSONException */
	public static void writeGameTemplateAuditRecords(JSONWriter jw, List<GameTemplateAuditRecord> os) throws JSONException {
		jw.array();
		for (GameTemplateAuditRecord o : os)
			writeGameTemplateAuditRecord(jw, o);
		jw.endArray();
	}
	/** write GameTemplateAuditRecord object for user 
	 * @throws JSONException */
	public static void writeGameTemplateAuditRecord(JSONWriter jw, GameTemplateAuditRecord o) throws JSONException {
		jw.object();
		if (o.getAccountKey()!=null) {
			jw.key(ACCOUNT_KEY);
			jw.value(o.getAccountKey().getName());
		}
		if (o.getClientIp()!=null) {
			jw.key(CLIENT_IP);
			jw.value(o.getClientIp());
		}
		if (o.getDetailsJson()!=null) {
			jw.key(DETAILS_JSON);
			jw.value(o.getDetailsJson());
		}
		if (o.getGameInstanceFactoryKey()!=null) {
			jw.key(GAME_INSTANCE_FACTORY_KEY);
			jw.value(KeyFactory.keyToString(o.getGameInstanceFactoryKey()));
		}
		if (o.getGameInstanceKey()!=null) {
			jw.key(GAME_INSTANCE_KEY);
			jw.value(o.getGameInstanceKey());
		}
		if (o.getGameTemplateId()!=null) {
			jw.key(GAME_TEMPLATE_ID);
			jw.value(o.getGameTemplateId());
		}
		if (o.getKey()!=null) {
			jw.key(KEY);
			jw.value(KeyFactory.keyToString(o.getKey()));
		}
		if (o.getLevel()!=null) {
			jw.key(LEVEL);
			jw.value(o.getLevel().toString());
		}
		if (o.getMessage()!=null) {
			jw.key(MESSAGE);
			jw.value(o.getMessage());
		}
		jw.key(TIME);
		jw.value(o.getTime());
		if (o.getType()!=null) {
			jw.key(TYPE);
			jw.value(o.getType().toString());
		}
		jw.endObject();
	}
}
