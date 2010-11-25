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
package uk.ac.horizon.ug.lobby.protocol;

import org.json.JSONObject;

/** Query from client to server, looking for a specific game.
 * 
 * @author cmg
 *
 */
public class GameQuery {
	/** protocol/object version */
	private int version;
	/** current version */
	public static final int CURRENT_VERSION = 1;
	/** client Id - required for interaction with previous join or secure ops */
	private String clientId;
	/** device Id - optional and unauthenticated - used as default clientId for anonymous use to avoid 'leaking' of slots on retry */
	private String deviceId;
	/** game template id (may be implicit in the way the query is sent) */
	private String gameTemplateId;
    /** client characteristics - JSON encoded object, i.e. set of property names & values */
    private String characteristicsJson;
    /** cache of parsed characteristics */
    private transient JSONObject characteristics;
    // now characteristic OSName
	/** client type - optional */
	//private String clientType;
	/** preferred/required client (template) name - optional */
	private String clientTitle;
    // now characteristic OSVersion
	/** client version - optional */
	//private Integer majorVersion;
	/** client version - optional */
	//private Integer minorVersion;
	/** client version - optional */
	//private Integer updateVersion;
	/** client location constraint - optional */
	private LocationConstraint locationConstraint;
	/** client current location - optional */
	private Integer latitudeE6;
	/** client current location - optional */
	private Integer longitudeE6;
	/** min game start time - optional */
	private TimeConstraint timeConstraint;
	/** include 'full' games */
	private Boolean includeFullGames;
	/** max results to return - optional */
	private Integer maxResults;
	/** cons */
	public GameQuery() {			
	}
	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
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
	 * @return the clientTitle
	 */
	public String getClientTitle() {
		return clientTitle;
	}
	/**
	 * @param clientTitle the clientTitle to set
	 */
	public void setClientTitle(String clientTitle) {
		this.clientTitle = clientTitle;
	}
	/**
	 * @return the locationConstraint
	 */
	public LocationConstraint getLocationConstraint() {
		return locationConstraint;
	}
	/**
	 * @param locationConstraint the locationConstraint to set
	 */
	public void setLocationConstraint(LocationConstraint locationConstraint) {
		this.locationConstraint = locationConstraint;
	}
	/**
	 * @return the latitudeE6
	 */
	public Integer getLatitudeE6() {
		return latitudeE6;
	}
	/**
	 * @param latitudeE6 the latitudeE6 to set
	 */
	public void setLatitudeE6(Integer latitudeE6) {
		this.latitudeE6 = latitudeE6;
	}
	/**
	 * @return the longitudeE6
	 */
	public Integer getLongitudeE6() {
		return longitudeE6;
	}
	/**
	 * @param longitudeE6 the longitudeE6 to set
	 */
	public void setLongitudeE6(Integer longitudeE6) {
		this.longitudeE6 = longitudeE6;
	}
	/**
	 * @return the timeConstraint
	 */
	public TimeConstraint getTimeConstraint() {
		return timeConstraint;
	}
	/**
	 * @param timeConstraint the timeConstraint to set
	 */
	public void setTimeConstraint(TimeConstraint timeConstraint) {
		this.timeConstraint = timeConstraint;
	}
	/**
	 * @return the includeFullGames
	 */
	public Boolean getIncludeFullGames() {
		return includeFullGames;
	}
	/**
	 * @param includeFullGames the includeFullGames to set
	 */
	public void setIncludeFullGames(Boolean includeFullGames) {
		this.includeFullGames = includeFullGames;
	}
	
	/**
	 * @return the maxResults
	 */
	public Integer getMaxResults() {
		return maxResults;
	}
	/**
	 * @param maxResults the maxResults to set
	 */
	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}
	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}
	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	/**
	 * @return the deviceId
	 */
	public String getDeviceId() {
		return deviceId;
	}
	/**
	 * @param deviceId the deviceId to set
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	/**
	 * @return the characteristicsJson
	 */
	public String getCharacteristicsJson() {
		return characteristicsJson;
	}
	/**
	 * @param characteristicsJson the characteristicsJson to set
	 */
	public void setCharacteristicsJson(String characteristicsJson) {
		this.characteristicsJson = characteristicsJson;
	}
	/**
	 * @return the characteristics
	 */
	public JSONObject getCharacteristics() {
		return characteristics;
	}
	/**
	 * @param characteristics the characteristics to set
	 */
	public void setCharacteristics(JSONObject characteristics) {
		this.characteristics = characteristics;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GameQuery [characteristicsJson=" + characteristicsJson
				+ ", clientId=" + clientId + ", clientTitle=" + clientTitle
				+ ", deviceId=" + deviceId + ", gameTemplateId="
				+ gameTemplateId + ", includeFullGames=" + includeFullGames
				+ ", latitudeE6=" + latitudeE6 + ", locationConstraint="
				+ locationConstraint + ", longitudeE6=" + longitudeE6
				+ ", maxResults=" + maxResults + ", timeConstraint="
				+ timeConstraint + ", version=" + version + "]";
	}
	
}
