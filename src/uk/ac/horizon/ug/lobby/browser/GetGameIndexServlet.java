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

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService; 
import com.google.appengine.api.users.UserServiceFactory; 
 
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/** 
 * Get Game (templates) info, for public browsing
 * 
 * @author cmg
 *
 */
@SuppressWarnings("serial")
public class GetGameIndexServlet extends HttpServlet implements Constants {
	static Logger logger = Logger.getLogger(GetGameIndexServlet.class.getName());
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		GameIndex gi = sc.getGameIndex();
		
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM "+GameTemplate.class.getName()+" x WHERE x."+VISIBILITY+" = :"+VISIBILITY+" ORDER BY x."+TITLE+" ASC");
			q.setParameter(VISIBILITY, GameTemplateVisibility.PUBLIC);
			List<GameTemplate> gameTemplates = (List<GameTemplate>)q.getResultList();
			
			List<GameTemplateInfo> gtis = new LinkedList<GameTemplateInfo>();
			for (GameTemplate gt : gameTemplates) {
				GameTemplateInfo gti = new GameTemplateInfo();
				gti.setGameTemplate(gt);
				q = em.createQuery("SELECT x FROM GameClientTemplate x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID);
				q.setParameter(GAME_TEMPLATE_ID, gt.getId());
				// Note: if the result list is just passed on without touching then
				// lazy loading doesn't happen until it is too late!
				List<GameClientTemplate> gcts = (List<GameClientTemplate>)q.getResultList();
				for (GameClientTemplate gct : gcts) 
					;
				gti.setGameClientTemplates(gcts);
				gti.setQueryUrl(makeQueryUrl(sc, gt));
				gtis.add(gti);
			}
			gi.setItems(gtis);
		}
		finally {
			em.close();
		}
		JSONUtils.sendGameIndex(resp, gi);
	}

	private static final String DEFAULT_BASE_URL = "http://localhost:8888/";
	private static final String QUERY_PATH = "browser/QueryGameTemplate/";
	private String makeQueryUrl(ServerConfiguration sc, GameTemplate gt) {
		StringBuilder sb = new StringBuilder();
		if (sc.getBaseUrl()==null) {
			logger.warning("Server BaseURL not configured");
			sb.append(DEFAULT_BASE_URL);
		}
		else {
			sb.append(sc.getBaseUrl());
			if (!sc.getBaseUrl().endsWith("/"))
				sb.append("/");			
		}
		sb.append(QUERY_PATH);
		sb.append(gt.getId());
		return sb.toString();
	}
}
