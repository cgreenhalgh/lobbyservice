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
package uk.ac.horizon.ug.lobby.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/** Additional (lobby) server-specific configuration.
 * 
 * @author cmg
 *
 */
@Entity
public class ServerConfiguration {
	/** key - autogenerated */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Key key; 
	/** public base URL for the server */
	private String baseUrl;
	/** global limit on GameInstanceFactory newInstanceTokensMax, i.e. how many GameInstances it can make at once */
	private Integer maxNewInstanceTokensMax;
	/** default maxNewInstanceTokensMax - 10 */
	public static int DEFAULT_MAX_NEW_INSTANCE_TOKENS_MAX = 10;
	/** global limit on GameInstanceFactory newInstanceTokensPerHour, i.e. how many GameInstances it can make per hour in the long run */
	private Integer maxNewInstanceTokensPerHour;
	/** default maxNewInstanceTokensPerHour - 10 */
	public static int DEFAULT_MAX_NEW_INSTANCE_TOKENS_PER_HOUR = 10;
	/** for networking only - GameIndex configuration */
	private transient GameIndex gameIndex;
	/** fixed name */
	private static final String CONFIGURATION_NAME = "configuration";
	/** get default key */
	public static Key getConfigurationKey() {
		return KeyFactory.createKey(ServerConfiguration.class.getSimpleName(), CONFIGURATION_NAME);
	}
	/**
	 * @return the key
	 */
	public Key getKey() {
		return key;
	}
	/**
	 * @param key the key to set
	 */
	public void setKey(Key key) {
		this.key = key;
	}
	/**
	 * @return the baseUrl
	 */
	public String getBaseUrl() {
		return baseUrl;
	}
	/**
	 * @param baseUrl the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	/**
	 * @return the gameIndex
	 */
	public GameIndex getGameIndex() {
		return gameIndex;
	}
	/**
	 * @param gameIndex the gameIndex to set
	 */
	public void setGameIndex(GameIndex gameIndex) {
		this.gameIndex = gameIndex;
	}
	/**
	 * @return the maxNewInstanceTokensMax
	 */
	public int getMaxNewInstanceTokensMax() {
		if (maxNewInstanceTokensMax==null)
			return DEFAULT_MAX_NEW_INSTANCE_TOKENS_MAX;
		return maxNewInstanceTokensMax;
	}
	/**
	 * @param maxNewInstanceTokensMax the maxNewInstanceTokensMax to set
	 */
	public void setMaxNewInstanceTokensMax(int maxNewInstanceTokensMax) {
		this.maxNewInstanceTokensMax = maxNewInstanceTokensMax;
	}
	/**
	 * @return the maxNewInstanceTokensPerHour
	 */
	public int getMaxNewInstanceTokensPerHour() {
		if (maxNewInstanceTokensPerHour==null)
			return DEFAULT_MAX_NEW_INSTANCE_TOKENS_PER_HOUR;
		return maxNewInstanceTokensPerHour;
	}
	/**
	 * @param maxNewInstanceTokensPerHour the maxNewInstanceTokensPerHour to set
	 */
	public void setMaxNewInstanceTokensPerHour(int maxNewInstanceTokensPerHour) {
		this.maxNewInstanceTokensPerHour = maxNewInstanceTokensPerHour;
	}
}
