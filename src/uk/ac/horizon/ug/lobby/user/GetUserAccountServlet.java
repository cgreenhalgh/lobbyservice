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

/** 
 * Get all Accounts (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class GetUserAccountServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(GetUserAccountServlet.class.getName());
	
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
			Query q = em.createQuery("SELECT x FROM "+Account.class.getName()+" x WHERE x.userId = :userId");
			q.setParameter("userId", user.getUserId());
			List<Account> accounts = (List<Account>)q.getResultList();
			Account account = null;
			if (accounts.size()==0) {
				logger.info("Creating new Account for "+user.getUserId()+": email="+user.getEmail()+", nickname="+user.getNickname());
				account = new Account();
				account.setUserId(user.getUserId());
				account.setNickname(user.getNickname());
				// can't create by default
				account.setGameTemplateQuota(0);
				em.persist(account);
			}
			else {
				account = accounts.get(0);
				if (accounts.size()>1) {
					logger.warning("Found "+accounts.size()+" Accounts for userId "+user.getUserId());
				}
			}
			
			resp.setCharacterEncoding(ENCODING);
			resp.setContentType(JSON_MIME_TYPE);		
			Writer w = new OutputStreamWriter(resp.getOutputStream(), ENCODING);
			JSONWriter jw = new JSONWriter(w);
			try {
				jw.object();
				jw.key("userId");
				jw.value(account.getUserId());
				jw.key("nickname");
				jw.value(account.getNickname());
				jw.key("gameTemplateQuota");
				jw.value(account.getGameTemplateQuota());
				jw.endObject();
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
