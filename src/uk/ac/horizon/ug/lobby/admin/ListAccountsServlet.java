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
public class ListAccountsServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(ListAccountsServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//logger.info("Get: contextPath="+req.getContextPath()+", pathInfo="+req.getPathInfo()+", queryString="+req.getQueryString());
		
		EntityManager em = EMF.get().createEntityManager();
		try {
			resp.setCharacterEncoding(ENCODING);
			resp.setContentType(JSON_MIME_TYPE);		
			Writer w = new OutputStreamWriter(resp.getOutputStream(), ENCODING);
			JSONWriter jw = new JSONWriter(w);
			try {
				jw.array();
				Query q = em.createQuery("SELECT x FROM "+Account.class.getName()+" x ORDER BY x.gameTemplateQuota DESC, x.userId ASC");
				List<Account> accounts = (List<Account>)q.getResultList();
				for (Account account : accounts) {
					jw.object();
					jw.key("userId");
					jw.value(account.getUserId());
					jw.key("nickname");
					jw.value(account.getNickname());
					jw.key("gameTemplateQuota");
					jw.value(account.getGameTemplateQuota());
					jw.endObject();
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
