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
	/** title  (cf. RSS2.0 item, required unless description present)*/
	private String title;
	/** web url path component - optional; should be unique */
	private String urlName;
	/** description (cf. RSS2.0 item, required unless title present) */
	private String description;
	/** link (URL) - to human-readable information about the game  (cf. RSS2.0 item, required) */
	private String link;
	/** image url - thumbnail for index (cf. RSS2.0 channel) */
	private String imageUrl;
	/** (default) language code (cf. RSS2.0 item) */
	private String language;
	/** owner Account */
	private Key ownerId;
	/** visibility - i.e. seen by browsers or not*/
	private GameTemplateVisibility visibility;
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
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}
	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
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
	 * @return the visibility
	 */
	public GameTemplateVisibility getVisibility() {
		return visibility;
	}
	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(GameTemplateVisibility visibility) {
		this.visibility = visibility;
	}
	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}
	/**
	 * @param link the link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}
	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}
	/**
	 * @param imageUrl the imageUrl to set
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	/**
	 * @return the urlName
	 */
	public String getUrlName() {
		return urlName;
	}
	/**
	 * @param urlName the urlName to set
	 */
	public void setUrlName(String urlName) {
		this.urlName = urlName;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GameTemplate [description=" + description + ", imageUrl="
				+ imageUrl + ", key=" + key + ", language=" + language
				+ ", link=" + link + ", ownerId=" + ownerId + ", title="
				+ title + ", visibility=" + visibility + "]";
	}
	
}
