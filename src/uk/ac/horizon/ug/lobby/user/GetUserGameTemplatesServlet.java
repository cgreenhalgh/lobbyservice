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
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplate;

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class GetUserGameTemplatesServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(GetUserGameTemplatesServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//logger.info("Get: contextPath="+req.getContextPath()+", pathInfo="+req.getPathInfo()+", queryString="+req.getQueryString());
        UserService userService = UserServiceFactory.getUserService(); 
        
        if (req.getUserPrincipal() == null) { 
        	logger.warning("getUserPrinciple failed");
        	resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User is not authenticated");
        	return;
        }
        User user = userService.getCurrentUser();
        if (user==null) {
        	logger.warning("getCurrentUser failed");
        	resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User is not authenticated");
        	return;
        }
        
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM "+Account.class.getName()+" x WHERE x."+USER_ID+" = :"+USER_ID);
			q.setParameter(USER_ID, user.getUserId());
			Account account = (Account)q.getSingleResult();
			q = em.createQuery("SELECT x FROM "+GameTemplate.class.getName()+" x WHERE x."+OWNER_ID+" = :"+OWNER_ID);
			q.setParameter(OWNER_ID, account.getKey());
			List<GameTemplate> gameTemplates = (List<GameTemplate>)q.getResultList();
			
			resp.setCharacterEncoding(ENCODING);
			resp.setContentType(JSON_MIME_TYPE);		
			Writer w = new OutputStreamWriter(resp.getOutputStream(), ENCODING);
			JSONWriter jw = new JSONWriter(w);
			try {
				jw.array();
				for (GameTemplate gameTemplate : gameTemplates) {
					jw.key(ID);
					jw.value(gameTemplate.getId());
					jw.key(TITLE);
					jw.value(gameTemplate.getTitle());
					jw.key(DESCRIPTION);
					jw.value(gameTemplate.getDescription());
					jw.key(LANG);
					jw.value(gameTemplate.getLang());
					jw.key(CLIENT_TEMPLATES);
					Query q2 = em.createQuery("SELECT x FROM "+GameClientTemplate.class.getName()+" x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID);
					q.setParameter(GAME_TEMPLATE_ID, gameTemplate.getId());
					List<GameClientTemplate> gameClientTemplates = (List<GameClientTemplate>)q.getResultList();
					jw.array();
					for (GameClientTemplate gameClientTemplate : gameClientTemplates) {
						// TODO
					}
					jw.endArray();
				}
				jw.endArray();
			} catch (JSONException je) {
				throw new IOException(je);
			}
			w.close();
		}
		finally {
			em.close();
		}
	}
}
