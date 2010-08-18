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

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplate;

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
		if (gameTemplate.getLang()!=null) {
			jw.key(LANG);
			jw.value(gameTemplate.getLang());
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
					jw.value(gameClientTemplate.getClientType());
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
			else if (key.equals(LANG))
				gt.setLang(json.getString(LANG));
			else if (key.equals(ID))
				gt.setId(json.getString(ID));
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
				gct.setClientType(json.getString(CLIENT_TYPE));
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
}
