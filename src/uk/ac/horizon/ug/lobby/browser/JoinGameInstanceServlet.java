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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.HttpUtils;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameJoinRequest;
import uk.ac.horizon.ug.lobby.protocol.GameQuery;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.protocol.LocationConstraint;
import uk.ac.horizon.ug.lobby.protocol.TimeConstraint;
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

		EntityManager em = EMF.get().createEntityManager();
		try {
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			GameJoinRequest gjreq = JSONUtils.parseGameJoinRequest(json);
			
			logger.info("GameJoinRequest "+gjreq);
			GameInstance gi = getGameInstance(req, em);
			
			// validate 
			if (gjreq.getType()==null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"request type must be specified");
				return;
			}
			
			List<GameClientTemplate> gcts = getGameClientTemplates(em, gjreq, gi.getGameTemplateId());

			if (gcts.size()==0) {
				logger.info("GameInstance/Template "+gi.getGameTemplateId()+" does not support any client(s) specified");
				// no matching client - so can't play
				// TODO resp.
				return;
			}
			GameClientTemplate gct = null;
			if (gcts.size()>1) {
				logger.warning("Found "+gcts.size()+" possible client templates - taking first");
				// TODO feedback?
			}
			gct = gcts.get(0);

			logger.info("Join using client "+gct);
			// response
			// TODO
			
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		finally {		
			em.close();
		}
	}
	private List<GameClientTemplate> getGameClientTemplates(EntityManager em,
			GameJoinRequest gjreq, String gameTemplateId) {
		return QueryGameTemplateServlet.getGameClientTemplates(em, gjreq.getClientTitle(), gjreq.getClientType(), gameTemplateId, gjreq.getMajorVersion(), gjreq.getMinorVersion(), gjreq.getUpdateVersion());
	}
}
