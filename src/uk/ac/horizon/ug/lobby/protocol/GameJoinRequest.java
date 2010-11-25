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

import uk.ac.horizon.ug.lobby.model.GameTemplateVisibility;

/** Query from client to server, looking for a specific game.
 * 
 * @author cmg
 *
 */
public class GameJoinRequest {
	/** protocol/object version */
	private int version;
	/** time - optional for duplicate request/replay detection */
	private Long time;
	/** sequence - optional for duplicate request/replay detection */
	private Integer seqNo;
	/** client Id - required for interaction with previous join or secure ops */
	private String clientId;
    /** nickname - optional */
    private String nickname;
	/** device Id - optional and unauthenticated - used as default clientId for anonymous use to avoid 'leaking' of slots on retry */
	private String deviceId;
	/** game slot id - required for interaction with previous join */
	private String gameSlotId; 
	/** join type */
	private GameJoinRequestType type;
	/** current version */
	public static final int CURRENT_VERSION = 1;
    /** client characteristics - JSON encoded object, i.e. set of property names & values */
    private String characteristicsJson;
    /** cache of parsed characteristics */
    private transient JSONObject characteristics;
    // now characteristics OSName
	/** client type - required */
	//private String clientType;
	/** preferred/required client (template) name - optional */
	private String clientTitle;
    // now characteristics OSVersion
	/** client version - required (default 0) */
	//private Integer majorVersion;
	/** client version - required (default 0) */
	//private Integer minorVersion;
	/** client version - required (default 0) */
	//private Integer updateVersion;
	/** client current location - optional */
	private Integer latitudeE6;
	/** client current location - optional */
	private Integer longitudeE6;
	/** start time - required when using a GameInstanceFactory (NEW_INSTANCE) */
	private Long newInstanceStartTime;
	/** suggested visibility of created game (HIDDEN -> personal?!) - optional, only when using a GameInstanceFactory (NEW_INSTANCE) */
	private GameTemplateVisibility newInstanceVisibility;
	/** cons */
	public GameJoinRequest() {			
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
	 * @return the time
	 */
	public Long getTime() {
		return time;
	}
	/**
	 * @param time the time to set
	 */
	public void setTime(Long time) {
		this.time = time;
	}
	/**
	 * @return the seqNo
	 */
	public Integer getSeqNo() {
		return seqNo;
	}
	/**
	 * @param seqNo the seqNo to set
	 */
	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
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
	 * @return the gameSlotId
	 */
	public String getGameSlotId() {
		return gameSlotId;
	}
	/**
	 * @param gameSlotId the gameSlotId to set
	 */
	public void setGameSlotId(String gameSlotId) {
		this.gameSlotId = gameSlotId;
	}
	/**
	 * @return the type
	 */
	public GameJoinRequestType getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(GameJoinRequestType type) {
		this.type = type;
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
	 * @return the newInstanceStartTime
	 */
	public Long getNewInstanceStartTime() {
		return newInstanceStartTime;
	}
	/**
	 * @param newInstanceStartTime the newInstanceStartTime to set
	 */
	public void setNewInstanceStartTime(Long newInstanceStartTime) {
		this.newInstanceStartTime = newInstanceStartTime;
	}
	/**
	 * @return the newInstanceVisibility
	 */
	public GameTemplateVisibility getNewInstanceVisibility() {
		return newInstanceVisibility;
	}
	/**
	 * @param newInstanceVisibility the newInstanceVisibility to set
	 */
	public void setNewInstanceVisibility(
			GameTemplateVisibility newInstanceVisibility) {
		this.newInstanceVisibility = newInstanceVisibility;
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
		return "GameJoinRequest [characteristicsJson=" + characteristicsJson
				+ ", clientId=" + clientId + ", clientTitle=" + clientTitle
				+ ", deviceId=" + deviceId + ", gameSlotId=" + gameSlotId
				+ ", latitudeE6=" + latitudeE6 + ", longitudeE6=" + longitudeE6
				+ ", newInstanceStartTime=" + newInstanceStartTime
				+ ", newInstanceVisibility=" + newInstanceVisibility
				+ ", nickname=" + nickname + ", seqNo=" + seqNo + ", time="
				+ time + ", type=" + type + ", version=" + version + "]";
	}
}
