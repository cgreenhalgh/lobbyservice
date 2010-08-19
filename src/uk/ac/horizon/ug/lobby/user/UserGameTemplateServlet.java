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
import uk.ac.horizon.ug.lobby.HttpUtils;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
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
public class UserGameTemplateServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(UserGameTemplateServlet.class.getName());

	public static GameTemplate getGameTemplate(HttpServletRequest req, EntityManager em) throws RequestException {
		Account account = AccountUtils.getAccount(req);
		return getGameTemplate(req, em, account);
	}
	public static GameTemplate getGameTemplate(HttpServletRequest req, EntityManager em, Account account) throws RequestException {
		String id = HttpUtils.getIdFromPath(req);
		
		Key key = GameTemplate.idToKey(id);
		GameTemplate gt = em.find(GameTemplate.class, key);
        if (gt==null)
        	throw new RequestException(HttpServletResponse.SC_NOT_FOUND, "GameTemplate "+id+" not found");
        
        if (!account.getKey().equals(gt.getOwnerId())) 
        	throw new RequestException(HttpServletResponse.SC_FORBIDDEN, "User is not owner for template "+id);
        
        return gt;
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameTemplate gt = getGameTemplate(req, em);
			Query q = em.createQuery("SELECT x FROM GameClientTemplate x WHERE x.gameTemplateId = :gameTemplateId ORDER BY x.title ASC");
			q.setParameter(GAME_TEMPLATE_ID, gt.getId());
			List<GameClientTemplate> gameClientTemplates = (List<GameClientTemplate>)q.getResultList();
			GameTemplateInfo gti = new GameTemplateInfo();
			gti.setGameTemplate(gt);
			gti.setGameClientTemplates(gameClientTemplates);
			JSONUtils.sendGameTemplate(resp, gti);
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
		}
		finally {
			em.close();
		}
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Account account = null;
		try {
			account = AccountUtils.getAccount(req);
		} catch (RequestException e1) {
			resp.sendError(e1.getErrorCode(), e1.getMessage());
			return;
		}
		EntityManager em = EMF.get().createEntityManager();
		GameTemplateInfo gti = null;
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			GameTemplate gt = getGameTemplate(req, em, account);	
			BufferedReader br = req.getReader();
			String line = br.readLine();
			JSONObject json = new JSONObject(line);
			gti = JSONUtils.parseGameTemplateInfo(json);
			GameTemplate ngt = gti.getGameTemplate();
			
			String id = gt.getId();
			if (ngt.getId()!=null && !id.equals(ngt.getId()))
				throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "GameTemplate id in URL ("+gt.getId()+") does not match id in data ("+ngt.getId()+")");
			if (ngt.getId()==null)
				ngt.setId(id);
			ngt.setOwnerId(account.getKey());
			Key key = ngt.getKey();

			logger.info("Update GameTemplate "+gt+" -> "+ngt);
			em.merge(ngt);
			// can't em.flush(); - "this operation requires a transaction yet it is not active
			
			if (gti.getGameClientTemplates()!=null) {				
				Query q = em.createQuery("SELECT x FROM GameClientTemplate x WHERE x.gameTemplateId = :gameTemplateId");
				q.setParameter(GAME_TEMPLATE_ID, gt.getId());
				List<GameClientTemplate> gcts = (List<GameClientTemplate>)q.getResultList();
				for (GameClientTemplate gct : gcts) {
					em.remove(gct);				
				}
				logger.info("Delete "+gcts.size()+" old GameClientTemplates");
				et.commit();
				et.begin();

				int i = 1;
				for (GameClientTemplate gameClientTemplate : gti.getGameClientTemplates()) {
					// fill in missing info
					gameClientTemplate.setGameTemplateId(id);
					gameClientTemplate.setKey(KeyFactory.createKey(key, GameClientTemplate.class.getSimpleName(), i++));
					logger.info("Creating new GameClientTemplate "+gameClientTemplate);
					em.persist(gameClientTemplate);
				}
			}
			et.commit();
		} catch (RequestException e) {
			resp.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (JSONException e) {
			logger.warning(e.toString());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
		finally {		
			if (et.isActive()) {
				logger.warning("rolling back active transaction");
				et.rollback();
			}
			em.close();
		}
		JSONUtils.sendGameTemplate(resp, gti);
	}
}
