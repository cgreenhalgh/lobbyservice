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

/**
 * @author cmg
 *
 */
@Entity
public class GameTemplate {
	/** globally unique (random) ID and primary key */
	@Id
	private Key key;
	/** title */
	private String title;
	/** description */
	private String description;
	/** (default) language code */
	private String lang;
	/** owner Account */
	private Key ownerId;
	public static Key idToKey(String id) {
		return KeyFactory.createKey(GameTemplate.class.getSimpleName(), id);
	}
	/**
	 */
	public GameTemplate() {
		super();
	}
	/**
	 * @param id
	 */
	public GameTemplate(String id) {
		super();
		this.key = idToKey(id);
	}
	/**
	 * @return the id
	 */
	public String getId() {
		if (key==null)
			return null;
		return key.getName();
	}
	public void setId(String id) {
		key = idToKey(id);
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}
	/**
	 * @param lang the lang to set
	 */
	public void setLang(String lang) {
		this.lang = lang;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GameTemplate [description=" + description + ", key=" + key
				+ ", lang=" + lang + ", ownerId=" + ownerId + ", title="
				+ title + "]";
	}
	
}
