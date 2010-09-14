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
package uk.ac.horizon.ug.lobby.server;

import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplateUrlName;
import uk.ac.horizon.ug.lobby.user.AddGameTemplateServlet;

import com.google.appengine.api.datastore.Key;

/**
 * @author cmg
 *
 */
public class UrlNameUtils implements Constants {
	static Logger logger = Logger.getLogger(UrlNameUtils.class.getName());
	/** map urlName to GameTemplate Key */
	public static Key getGameTemplateKey(String urlName) {
		Key mapKey = GameTemplateUrlName.urlNameToKey(urlName);
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameTemplateUrlName gtun = em.find(GameTemplateUrlName.class, mapKey);
			if (gtun!=null)
				return gtun.getGameTemplateKey();
		}
		finally {
			em.close();
		}
		return null;
	}
	/** update UrlName mapping for GameTemplate(s) */
	public static void updateGameTemplateUrlName(GameTemplate gameTemplate) {
		// check new url name
		String urlName = gameTemplate.getUrlName();
		// quick check on old url name
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		GameTemplateUrlName gtun = null;
		try {
			if (urlName!=null) {
				Key key = GameTemplateUrlName.urlNameToKey(urlName);
				et.begin();
				em.find(GameTemplateUrlName.class, key);
				if (gtun!=null) {
					// still ours?
					if (!gtun.getGameTemplateKey().equals(gameTemplate.getKey())) {
						logger.warning("GameTemplateUrlName '"+urlName+"' owned by another template: "+gtun.getGameTemplateKey().getName());
						gtun = null;
					}
					else {
						// already ours
					}
					et.rollback();
				}
				else {
					// create atomic
					gtun = new GameTemplateUrlName();
					gtun.setKey(key);
					gtun.setGameTemplateKey(gameTemplate.getKey());
					em.persist(gtun);
					et.commit();
				}
			}
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
		// tidy up
		em = EMF.get().createEntityManager();
		try {
			Query q = em.createQuery("SELECT x FROM GameTemplateUrlName x WHERE x."+GAME_TEMPLATE_KEY+" = :"+GAME_TEMPLATE_KEY);
			List<GameTemplateUrlName> gtuns = (List<GameTemplateUrlName>)q.getResultList();
			for (GameTemplateUrlName gtun2 : gtuns) {
				if (urlName==null || !urlName.equals(gtun2.getKey().getName()))
					// not sure how to play this with transactions becuase of lazy load of list vs remove
					em.remove(gtun2);
			}
		}
		finally {
			em.close();
		}
		// TODO Auto-generated method stub
		
	}

}
