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

/**
 * @author cmg
 *
 */
@Entity
public class GameInstance {
	/** key - autogenerated */
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Key key; 
    /** game template id */
    private String gameTemplateId;
    /** server */
    private Key gameServerId;
    /** title */
    private String title;
    /** start time */
    private long startTime;
    /** end time */
    private long endTime;
    /** latitude */
    private int latitudeE6;
    /** longitude */
    private int longitudeE6;
    /** radius metres (or 0) */
    private double radiusMetres;
    /** current/last known status */
    private GameInstanceStatus status;
    /** game instance base url (several games may be hosted by the same GameServer) */
    private String baseUrl;
    /** cons */
    public GameInstance() {
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
	 * @return the gameServerId
	 */
	public Key getGameServerId() {
		return gameServerId;
	}
	/**
	 * @param gameServerId the gameServerId to set
	 */
	public void setGameServerId(Key gameServerId) {
		this.gameServerId = gameServerId;
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
	 * @return the latitudeE6
	 */
	public int getLatitudeE6() {
		return latitudeE6;
	}
	/**
	 * @param latitudeE6 the latitudeE6 to set
	 */
	public void setLatitudeE6(int latitudeE6) {
		this.latitudeE6 = latitudeE6;
	}
	/**
	 * @return the longitudeE6
	 */
	public int getLongitudeE6() {
		return longitudeE6;
	}
	/**
	 * @param longitudeE6 the longitudeE6 to set
	 */
	public void setLongitudeE6(int longitudeE6) {
		this.longitudeE6 = longitudeE6;
	}
	/**
	 * @return the radiusMetres
	 */
	public double getRadiusMetres() {
		return radiusMetres;
	}
	/**
	 * @param radiusMetres the radiusMetres to set
	 */
	public void setRadiusMetres(double radiusMetres) {
		this.radiusMetres = radiusMetres;
	}
	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}
	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	/**
	 * @return the status
	 */
	public GameInstanceStatus getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(GameInstanceStatus status) {
		this.status = status;
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
    
}
