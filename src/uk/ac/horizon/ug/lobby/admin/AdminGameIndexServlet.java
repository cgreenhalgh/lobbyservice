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
package uk.ac.horizon.ug.lobby.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Get/update lobby config, in particular Raw GameIndex information.
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class AdminGameIndexServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(AdminGameIndexServlet.class.getName());
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		JSONUtils.sendServerConfiguration(resp, sc);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// force create
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		GameIndex gi = sc.getGameIndex();
		
		EntityManager em = EMF.get().createEntityManager();
		try {
			BufferedReader r = req.getReader();
			String line = r.readLine();
			JSONObject json = new JSONObject(line);

			Iterator keys = json.keys();
			while(keys.hasNext()) {
				String key = (String)keys.next();
				if (key.equals(DESCRIPTION))
					gi.setDescription(json.getString(DESCRIPTION));
				else if (key.equals(DOCS))
					gi.setDocs(json.getString(DOCS));
				else if (key.equals(GENERATOR))
					gi.setGenerator(json.getString(GENERATOR));
				else if (key.equals(IMAGE_URL))
					gi.setImageUrl(json.getString(key));
				else if (key.equals(LANGUAGE))
					gi.setLanguage(json.getString(key));
				else if (key.equals(LAST_BUILD_DATE))
					gi.setLastBuildDate(json.getLong(key));
				else if (key.equals(LINK))
					gi.setLink(json.getString(key));
				else if (key.equals(TITLE))
					gi.setTitle(json.getString(key));
				else if (key.equals(TTL_MINUTES))
					gi.setTtlMinutes(json.getInt(key));
				else if (key.equals(BASE_URL))
					sc.setBaseUrl(json.getString(key));
				else if (key.equals(MAX_NEW_INSTANCE_TOKENS_MAX))
					sc.setMaxNewInstanceTokensMax(json.getInt(key));
				else if (key.equals(MAX_NEW_INSTANCE_TOKENS_PER_HOUR))
					sc.setMaxNewInstanceTokensPerHour(json.getInt(key));
				else if (key.equals(VERSION)) {
					// ignore?!
				}
				else 
					throw new JSONException("Unsupported key '"+key+"' in GameIndex: "+json);
			}
			// ? 
			em.merge(gi);
			em.merge(sc);
		} catch (JSONException e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		finally {
			em.close();
		}
		JSONUtils.sendServerConfiguration(resp, sc);
	}
}
