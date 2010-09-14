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
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateUrlName;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.server.UrlNameUtils;

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
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, je.toString());
			return;
		}
        
		try {
			Account account = AccountUtils.getAccount(req);
			handleAddGameTemplate(gameTemplateInfo, account);

			gameTemplateInfo.setIncludePrivateFields(true);
			// echo
			JSONUtils.sendGameTemplate(resp, gameTemplateInfo);
		}catch (RequestException re) {
			resp.sendError(re.getErrorCode(), re.getMessage());
			return;
		}
	}
	public static void testHandleAddGameTemplate(GameTemplateInfo gameTemplateInfo, Account account) throws RequestException {
		handleAddGameTemplate(gameTemplateInfo, account);
	}
	private static void handleAddGameTemplate(GameTemplateInfo gameTemplateInfo, Account account) throws RequestException {
		EntityManager em = EMF.get().createEntityManager();
		String id = gameTemplateInfo.getGameTemplate().getId();
		try {
			// Note: this is not entirely safe: concurrent adds won't be counted...
			Query q = em.createQuery("SELECT COUNT(x) FROM "+GameTemplate.class.getName()+" x WHERE x."+OWNER_ID+" = :"+OWNER_ID);
			q.setParameter(OWNER_ID, account.getKey());
			int count = (Integer)q.getSingleResult();

			if (account.getGameTemplateQuota() <= count) {
				String msg = "Account "+account.getUserId()+" ("+account.getNickname()+") cannot add GameTemplate: quota="+account.getGameTemplateQuota()+", existing templates="+count;
				logger.info(msg);
				throw new RequestException(HttpServletResponse.SC_FORBIDDEN, msg);
			}
		}
		finally {
			em.close();
		}
		
		// fill in missing info
		if (id==null) {
			id = GUIDFactory.newGUID();
			gameTemplateInfo.getGameTemplate().setId(id);
		}
		Key key = gameTemplateInfo.getGameTemplate().getKey();
		gameTemplateInfo.getGameTemplate().setOwnerId(account.getKey());

		em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		try {
			// atomic
			et.begin();

			if (em.find(GameTemplate.class, id)!=null) {
				String msg = "GameTemplate "+id+" already exists";
				logger.info(msg+" - add request by Account "+account.getUserId()+" ("+account.getNickname()+"");
				throw new RequestException(HttpServletResponse.SC_CONFLICT, msg);
			}
			em.persist(gameTemplateInfo.getGameTemplate());
			logger.info("Creating GameTemplate "+gameTemplateInfo.getGameTemplate());
			
			// client templates are part of GameTemplate entity group (and so same transaction)
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
			et.commit();
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
		UrlNameUtils.updateGameTemplateUrlName(gameTemplateInfo.getGameTemplate());
	}
}
