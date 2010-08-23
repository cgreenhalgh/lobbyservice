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

import uk.ac.horizon.ug.lobby.model.GameClientType;

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
	/** game template id (may be implicit in the way the query is sent) */
	private String gameTemplateId;
	/** client type - optional */
	private GameClientType clientType;
	/** preferred/required client (template) name - optional */
	private String clientTitle;
	/** client version - optional */
	private Integer majorVersion;
	/** client version - optional */
	private Integer minorVersion;
	/** client version - optional */
	private Integer updateVersion;
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
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GameQuery [clientTitle=" + clientTitle + ", clientType="
				+ clientType + ", gameTemplateId=" + gameTemplateId
				+ ", includeFullGames=" + includeFullGames + ", latitudeE6="
				+ latitudeE6 + ", locationConstraint=" + locationConstraint
				+ ", longitudeE6=" + longitudeE6 + ", majorVersion="
				+ majorVersion + ", minorVersion=" + minorVersion
				+ ", timeConstraint=" + timeConstraint + ", updateVersion="
				+ updateVersion + ", version=" + version + "]";
	}
	
}
