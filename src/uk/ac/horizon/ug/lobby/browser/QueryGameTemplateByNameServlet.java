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
 
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.HttpUtils;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.protocol.GameQuery;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.server.UrlNameUtils;

/** 
 * Get Game (templates) info, for public browsing
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class QueryGameTemplateByNameServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(QueryGameTemplateByNameServlet.class.getName());

	private GameTemplate getGameTemplateByName(HttpServletRequest req) throws RequestException {
		String name = HttpUtils.getIdFromPath(req);		
		Key key = UrlNameUtils.getGameTemplateKey(name);
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameTemplate gt = key!=null ? em.find(GameTemplate.class, key) : null;
	        if (gt==null)
	        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameTemplate (name) "+name+" not found");
	        return gt;
		}
		finally {
			em.close();
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// parse request
		String line = null;
		String auth = null;
		GameQuery gq = null;
		GameTemplate gt = null;
		try {
			BufferedReader br = req.getReader();
			line = br.readLine();
			JSONObject json = new JSONObject(line);
			gq = JSONUtils.parseGameQuery(json);
			// second line is digital signature (if given)
			auth = br.readLine();
			
			logger.info("GameQuery "+gq);
			gt = getGameTemplateByName(req);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		try {
			JoinUtils.JoinAuthInfo jai = JoinUtils.authenticateOptional(gq.getClientId(), gq.getDeviceId(), line, auth);
			GameIndex gindex = QueryGameTemplateServlet.handleGameQuery(gq, gt, jai);
			// response
			JSONUtils.sendGameIndex(resp, gindex);
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
	}
}
