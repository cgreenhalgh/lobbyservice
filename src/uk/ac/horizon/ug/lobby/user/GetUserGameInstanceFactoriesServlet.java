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
package uk.ac.horizon.ug.lobby.user;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService; 
import com.google.appengine.api.users.UserServiceFactory; 
 
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class GetUserGameInstanceFactoriesServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(GetUserGameInstanceFactoriesServlet.class.getName());
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		Account account;
		try {
			account = AccountUtils.getAccount(req);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		}

		
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameTemplate gameTemplate = UserGameTemplateServlet.getGameTemplate(req, em, account);

			Query q = em.createQuery("SELECT x FROM GameInstanceFactory x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID);
			q.setParameter(GAME_TEMPLATE_ID, gameTemplate.getId());
			List<GameInstanceFactory> gameInstanceFactories = (List<GameInstanceFactory>)q.getResultList();
			
			Writer w = JSONUtils.getResponseWriter(resp);
			JSONWriter jw = new JSONWriter(w);
			try {
				JSONUtils.writeGameInstanceFactories(jw, gameInstanceFactories);
			} catch (JSONException je) {
				throw new IOException(je);
			}
			w.close();
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		}
		finally {
			em.close();
		}
	}
}
