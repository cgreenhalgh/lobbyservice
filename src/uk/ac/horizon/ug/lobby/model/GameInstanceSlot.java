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

/** A particular client's relationship to a particular GameInstance.
 * 
 * @author cmg
 *
 */
@Entity
public class GameInstanceSlot {
	/** key - parent is GameInstance key. Name is Slot ID. */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Key key; 
    /** game instance id */
    private Key gameInstanceKey;
    /** client */
    private Key gameClientKey;
    /** account - optional */
    private Key accountKey;
	/** nickname for play in game */
	private String nickname;
    /** game template id (strictly redundant with gameInstance.gameTemplateId) */
    private String gameTemplateId;
    /** slot status */
    private GameInstanceSlotStatus status;
    /** client shared secret */
    private String clientSharedSecret;
    /** id to key */
    public static final Key idToKey(Key gameInstanceKey, String id) {
    	return KeyFactory.createKey(gameInstanceKey, GameInstanceSlot.class.getSimpleName(), id);
    }
    /** cons */
    public GameInstanceSlot() {    	
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
	 * @return the gameInstanceKey
	 */
	public Key getGameInstanceKey() {
		return gameInstanceKey;
	}
	/**
	 * @param gameInstanceKey the gameInstanceKey to set
	 */
	public void setGameInstanceKey(Key gameInstanceKey) {
		this.gameInstanceKey = gameInstanceKey;
	}
	/**
	 * @return the gameClientKey
	 */
	public Key getGameClientKey() {
		return gameClientKey;
	}
	/**
	 * @param gameClientKey the gameClientKey to set
	 */
	public void setGameClientKey(Key gameClientKey) {
		this.gameClientKey = gameClientKey;
	}
	/**
	 * @return the accountKey
	 */
	public Key getAccountKey() {
		return accountKey;
	}
	/**
	 * @param accountKey the accountKey to set
	 */
	public void setAccountKey(Key accountKey) {
		this.accountKey = accountKey;
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
	/**
	 * @return the status
	 */
	public GameInstanceSlotStatus getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(GameInstanceSlotStatus status) {
		this.status = status;
	}
	/**
	 * @return the clientSharedSecret
	 */
	public String getClientSharedSecret() {
		return clientSharedSecret;
	}
	/**
	 * @param clientSharedSecret the clientSharedSecret to set
	 */
	public void setClientSharedSecret(String clientSharedSecret) {
		this.clientSharedSecret = clientSharedSecret;
	}
	/**
	 * @return the nickname
	 */
	public String getNickname() {
		return nickname;
	}
	/**
	 * @param nickname the nickname to set
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
