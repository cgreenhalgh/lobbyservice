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

/** Lobby service user (including Game administrator).
 * 
 * @author cmg
 *
 */
@Entity
public class Account {
	/** key - autogenerated */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Key key; 
    /** opaque & persistent user id, for GAE from User.getUserId */
    private String userId;
    /** cached nickname */
    private String nickname;
    /** game template quota - total number */
    private int gameTemplateQuota;
    
	public static Key userIdToKey(String id) {
		return KeyFactory.createKey(Account.class.getSimpleName(), id);
	}
    /** cons */
    public Account() {    	
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
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
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
	/**
	 * @return the gameTemplateQuota
	 */
	public int getGameTemplateQuota() {
		return gameTemplateQuota;
	}
	/**
	 * @param gameTemplateQuota the gameTemplateQuota to set
	 */
	public void setGameTemplateQuota(int gameTemplateQuota) {
		this.gameTemplateQuota = gameTemplateQuota;
	}
    
}
