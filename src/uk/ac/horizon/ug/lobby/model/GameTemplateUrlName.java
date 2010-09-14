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
import javax.persistence.Id;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/** GameTemplate.urlName forwarding record - for consistency & speed.
 * @author cmg
 *
 */
@Entity
public class GameTemplateUrlName {
	/** key - name is urlName */
	@Id
	private Key key;
	/** game template key */
	private Key gameTemplateKey;
	/** urlName to key */
	public static Key urlNameToKey(String urlName) {
		return KeyFactory.createKey(GameTemplateUrlName.class.getSimpleName(), urlName);
	}
	/** cons */
	public GameTemplateUrlName() {}
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
	 * @return the gameTemplateKey
	 */
	public Key getGameTemplateKey() {
		return gameTemplateKey;
	}
	/**
	 * @param gameTemplateKey the gameTemplateKey to set
	 */
	public void setGameTemplateKey(Key gameTemplateKey) {
		this.gameTemplateKey = gameTemplateKey;
	}
	
}
