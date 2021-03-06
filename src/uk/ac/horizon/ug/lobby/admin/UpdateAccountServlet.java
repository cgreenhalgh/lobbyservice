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
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONWriter;
import org.json.JSONObject;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Update account (admin view).
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class UpdateAccountServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(UpdateAccountServlet.class.getName());
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		//logger.info("Get: contextPath="+req.getContextPath()+", pathInfo="+req.getPathInfo()+", queryString="+req.getQueryString());
		
		String userId = null;
		JSONObject json = null;
		try {
			//tx.begin();
			BufferedReader r = req.getReader();
			String line = r.readLine();
			//logger.info("UpdateAccount(1): "+line);
			// why does this seem to read {} ??
			//JSONObject json = new JSONObject(req.getReader());
			json = new JSONObject(line);
			//logger.info("UpdateAccount: "+json);
			userId = json.getString(USER_ID);
		} catch (JSONException je) {
			throw new IOException(je);
		}
		
		try {
			
			Account account = updateAccount(userId, json);
			JSONUtils.sendAccount(resp, account);

		} catch (JSONException je) {
			throw new IOException(je);
		}

	}

	public static Account testUpdateAccount(String userId, JSONObject json) throws JSONException {
		return updateAccount(userId, json);
	}
	private static Account updateAccount(String userId, JSONObject json) throws JSONException {
		EntityManager em = EMF.get().createEntityManager();
		//EntityTransaction tx = em.getTransaction();
		try {
			Account account = em.find(Account.class, Account.userIdToKey(userId));
			if (json.has(GAME_TEMPLATE_QUOTA)) {
				account.setGameTemplateQuota(json.getInt(GAME_TEMPLATE_QUOTA));
				logger.info("Updated account "+userId+" gameTemplateQuota to "+account.getGameTemplateQuota());
				//em.merge(account);
			}		
			// check/update 
			return account;
			//em.flush();
			//tx.commit();
		}
		finally {
			em.close();
		}
	}
}
