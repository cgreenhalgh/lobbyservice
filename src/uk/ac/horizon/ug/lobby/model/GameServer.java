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

/** Information about a single game "server" instance, e.g. a single EC2 instance hosting a web server, 
 * a Google App Engine application, a single managed server hosting a web server.
 * 
 * @author cmg
 *
 */
@Entity
public class GameServer {
	/** key - autogenerated */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Key key; 
	/** owner Account */
	private Key ownerId;
	/** title/name */
	private String title;
    /** server type, i.e. protocol/interaction paradigm e.g. EXPLODING_PLACES */
    private GameServerType type;
    /** supported game template id */
    private String gameTemplateId;
    /** base URL, e.g. http://host:port/ */
    private String baseUrl;
    /** shared secret */
    private String lobbySharedSecret;
    /** last known status */
    private GameServerStatus lastKnownStatus;
    /** last known status time */
    private long lastKnownStatusTime;
    /** target status, i.e. what it should be doing */
    private GameServerStatus targetStatus;
	/**
	 * 
	 */
	public GameServer() {
		super();
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
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the type
	 */
	public GameServerType getType() {
		return type;
	}
	/**
	 * @return the ownerId
	 */
	public Key getOwnerId() {
		return ownerId;
	}
	/**
	 * @param ownerId the ownerId to set
	 */
	public void setOwnerId(Key ownerId) {
		this.ownerId = ownerId;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(GameServerType type) {
		this.type = type;
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
	 * @return the lobbySharedSecret
	 */
	public String getLobbySharedSecret() {
		return lobbySharedSecret;
	}
	/**
	 * @param lobbySharedSecret the lobbySharedSecret to set
	 */
	public void setLobbySharedSecret(String lobbySharedSecret) {
		this.lobbySharedSecret = lobbySharedSecret;
	}
	/**
	 * @return the lastKnownStatus
	 */
	public GameServerStatus getLastKnownStatus() {
		return lastKnownStatus;
	}
	/**
	 * @param lastKnownStatus the lastKnownStatus to set
	 */
	public void setLastKnownStatus(GameServerStatus lastKnownStatus) {
		this.lastKnownStatus = lastKnownStatus;
	}
	/**
	 * @return the lastKnownStatusTime
	 */
	public long getLastKnownStatusTime() {
		return lastKnownStatusTime;
	}
	/**
	 * @param lastKnownStatusTime the lastKnownStatusTime to set
	 */
	public void setLastKnownStatusTime(long lastKnownStatusTime) {
		this.lastKnownStatusTime = lastKnownStatusTime;
	}
	/**
	 * @return the targetStatus
	 */
	public GameServerStatus getTargetStatus() {
		return targetStatus;
	}
	/**
	 * @param targetStatus the targetStatus to set
	 */
	public void setTargetStatus(GameServerStatus targetStatus) {
		this.targetStatus = targetStatus;
	}
	/**
	 * @return the gameTemplateId
	 */
	public String getGameTemplateId() {
		return gameTemplateId;
	}
	/**
	 * @param gameTemplateId the gameTemplateId to set
	 */
	public void setGameTemplateId(String gameTemplateId) {
		this.gameTemplateId = gameTemplateId;
	}
    
}
