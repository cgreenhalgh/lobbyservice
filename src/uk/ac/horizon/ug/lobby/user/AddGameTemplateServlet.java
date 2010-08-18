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

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService; 
import com.google.appengine.api.users.UserServiceFactory; 
 
import java.io.BufferedReader;
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
import org.json.JSONObject;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class AddGameTemplateServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(AddGameTemplateServlet.class.getName());
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//logger.info("Get: contextPath="+req.getContextPath()+", pathInfo="+req.getPathInfo()+", queryString="+req.getQueryString());
        UserService userService = UserServiceFactory.getUserService(); 
        
        User user = userService.getCurrentUser();
        if (user==null) {
        	logger.warning("getCurrentUser failed");
        	resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User is not authenticated");
        	return;
        }

        GameTemplateInfo gameTemplateInfo = null;
		try {
			BufferedReader r = req.getReader();
			String line = r.readLine();
			// why does this seem to read {} ??
			//JSONObject json = new JSONObject(req.getReader());
			JSONObject json = new JSONObject(line);
			gameTemplateInfo = JSONUtils.parseGameTemplateInfo(json);
		}
		catch (JSONException je) {
			throw new IOException(je);
		}
        
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM "+Account.class.getName()+" x WHERE x."+USER_ID+" = :"+USER_ID);
			q.setParameter(USER_ID, user.getUserId());
			Account account = (Account)q.getSingleResult();
			q = em.createQuery("SELECT COUNT(x) FROM "+GameTemplate.class.getName()+" x WHERE x."+OWNER_ID+" = :"+OWNER_ID);
			q.setParameter(OWNER_ID, account.getKey());
			int count = (Integer)q.getSingleResult();

			if (account.getGameTemplateQuota() <= count) {
				String msg = "Account "+account.getUserId()+" ("+account.getNickname()+") cannot add GameTemplate: quota="+account.getGameTemplateQuota()+", existing templates="+count;
				logger.info(msg);
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
				return;
			}
			
			// fill in missing info
			String id = gameTemplateInfo.getGameTemplate().getId();
			if (id==null) {
				id = GUIDFactory.newGUID();
				gameTemplateInfo.getGameTemplate().setId(id);
			}
			Key key = gameTemplateInfo.getGameTemplate().getKey();
			gameTemplateInfo.getGameTemplate().setOwnerId(account.getKey());
			
			if (em.find(GameTemplate.class, id)!=null) {
				String msg = "GameTemplate "+id+" already exists";
				logger.info(msg+" - add request by Account "+account.getUserId()+" ("+account.getNickname()+"");
				resp.sendError(HttpServletResponse.SC_CONFLICT, msg);
				return;
			}
			em.persist(gameTemplateInfo.getGameTemplate());
			logger.info("Creating GameTemplate "+gameTemplateInfo.getGameTemplate());
			
			if (gameTemplateInfo.getGameClientTemplates()!=null) {
				int i=1;
				for (GameClientTemplate gameClientTemplate : gameTemplateInfo.getGameClientTemplates()) {
					// fill in missing info
					gameClientTemplate.setGameTemplateId(id);
					gameClientTemplate.setKey(KeyFactory.createKey(key, GameClientTemplate.class.getSimpleName(), i++));
					logger.info("Creating GameClientTemplate "+gameClientTemplate);
					em.persist(gameClientTemplate);
				}
			}
		}
		finally {
			em.close();
		}

		JSONUtils.sendGameTemplate(resp, gameTemplateInfo);
	}
}
