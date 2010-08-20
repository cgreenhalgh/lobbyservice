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
package uk.ac.horizon.ug.lobby;

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import uk.ac.horizon.ug.lobby.admin.AdminGameIndexServlet;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameIndex;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;

/**
 * @author cmg
 *
 */
public class ConfigurationUtils {
	static Logger logger = Logger.getLogger(ConfigurationUtils.class.getName());
	/** docs URL */
	public static final String DOCS_URL = "http://github.com/cgreenhalgh/lobbyservice";
	/** generator name */
	public static final String GENERATOR_NAME = "Horizon LobbyService 1.0";
	/** get configuration GameIndex */
	public static GameIndex getConfigurationGameIndex() {
		ServerConfiguration sc = getServerConfiguration();
		return sc.getGameIndex();
	}
	/** get ServerConfiguration including GameIndex */
	public static ServerConfiguration getServerConfiguration() {
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameIndex gi = em.find(GameIndex.class, GameIndex.getConfigurationKey());
			if (gi==null) {
				logger.info("Creating configuration GameIndex");
				gi = new GameIndex();
				gi.setKey(GameIndex.getConfigurationKey());
				em.persist(gi);
				// split transactions in case of add ServerConfiguration
			}
			gi.setDocs(DOCS_URL);
			gi.setGenerator(GENERATOR_NAME);
			gi.setVersion(GameIndex.CURRENT_VERSION);
			ServerConfiguration sc = em.find(ServerConfiguration.class, ServerConfiguration.getConfigurationKey());
			if (sc==null) {
				logger.info("Creating ServerConfiguration");
				sc = new ServerConfiguration();
				sc.setKey(ServerConfiguration.getConfigurationKey());
				em.persist(sc);
			}
			sc.setGameIndex(gi);
			return sc;
		}
		finally {			
			em.close();
		}
	}
}
