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

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * @author cmg
 *
 */
@PersistenceCapable
public class GameClient {
	/* Name is external ClientID (GUID) */
    @PrimaryKey 
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key; 
    /** ID - also key name, but separate to allow query without knowledge of Account ID */
    //private String id;
    /** account key - if client is associated with account */
    private Key accountKey;
    /** default nickname */
    private String nickname;
    /** shared secret */
    private String sharedSecret;
    /** client type, e.g. "Android" */
    private GameClientType clientType;
    /** min major version */
    private Integer majorVersion;
    /** min minor version (or 0) */
    private Integer minorVersion;
    /** min update (or 0) */
    private Integer updateVersion;
    /** IMEI */
    private String imei;
    /** generate key */
    public static final Key idToKey(String id) {
    	// No point making Account parent, and it makes the create new client have more of a race!
//    	if (accountKey!=null) 
//    		return KeyFactory.createKey(accountKey, GameClient.class.getSimpleName(), id);
//    	else
    	return KeyFactory.createKey(GameClient.class.getSimpleName(), id);
    }
    /** cons */
    public GameClient() {    	
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
	 * @return the id
	 */
	public String getId() {
		if (key!=null)
			return key.getName();
		else
			return null;
		//return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		if (id==null)
			key = null;
		else
			key = idToKey(id);
		//this.id = id;
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
	 * @return the sharedSecret
	 */
	public String getSharedSecret() {
		return sharedSecret;
	}
	/**
	 * @param sharedSecret the sharedSecret to set
	 */
	public void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}
	/**
	 * @return the clientType
	 */
	public GameClientType getClientType() {
		return clientType;
	}
	/**
	 * @param clientType the clientType to set
	 */
	public void setClientType(GameClientType clientType) {
		this.clientType = clientType;
	}
	/**
	 * @return the majorVersion
	 */
	public Integer getMajorVersion() {
		return majorVersion;
	}
	/**
	 * @param majorVersion the majorVersion to set
	 */
	public void setMajorVersion(Integer majorVersion) {
		this.majorVersion = majorVersion;
	}
	/**
	 * @return the minorVersion
	 */
	public Integer getMinorVersion() {
		return minorVersion;
	}
	/**
	 * @param minorVersion the minorVersion to set
	 */
	public void setMinorVersion(Integer minorVersion) {
		this.minorVersion = minorVersion;
	}
	/**
	 * @return the updateVersion
	 */
	public Integer getUpdateVersion() {
		return updateVersion;
	}
	/**
	 * @param updateVersion the updateVersion to set
	 */
	public void setUpdateVersion(Integer updateVersion) {
		this.updateVersion = updateVersion;
	}
	/**
	 * @return the imei
	 */
	public String getImei() {
		return imei;
	}
	/**
	 * @param imei the imei to set
	 */
	public void setImei(String imei) {
		this.imei = imei;
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
