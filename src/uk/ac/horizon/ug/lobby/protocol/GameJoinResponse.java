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

import java.util.Map;

import uk.ac.horizon.ug.lobby.model.GameClientType;

/** Query from client to server, looking for a specific game.
 * 
 * @author cmg
 *
 */
public class GameJoinResponse {
	/** protocol/object version */
	private int version;
	/** time - response generated */
	private Long time;
	/** client Id - required for interaction with previous join or secure ops */
	private String clientId;
	/** nickname for play in game */
	private String nickname;
	/** game slot id - required for interaction with previous join */
	private String gameSlotId; 
	/** join type */
	private GameJoinRequestType type;
	/** status */
	private GameJoinResponseStatus status;
	/** message - for user */
	private String message;
	/** time when you can (next) try to PLAY (or NEW_INSTANCE, if that failed on [say] quota) */
	private Long playTime;
	/** join Url - for NEW_INSTANCE response (if mapped to a GameInstance) for subsequent interaction */
	private String joinUrl;
	/** play Url - for the real server */
	private String playUrl;
	/** additional client-specific information */
	private Map<String,Object> playData;
	/** current version */
	public static final int CURRENT_VERSION = 1;
	/** cons */
	public GameJoinResponse() {			
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
	 * @return the status
	 */
	public GameJoinResponseStatus getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(GameJoinResponseStatus status) {
		this.status = status;
	}
	/**
	 * @return the playTime
	 */
	public Long getPlayTime() {
		return playTime;
	}
	/**
	 * @param playTime the playTime to set
	 */
	public void setPlayTime(Long playTime) {
		this.playTime = playTime;
	}
	/**
	 * @return the playUrl
	 */
	public String getPlayUrl() {
		return playUrl;
	}
	/**
	 * @param playUrl the playUrl to set
	 */
	public void setPlayUrl(String playUrl) {
		this.playUrl = playUrl;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the playData
	 */
	public Map<String, Object> getPlayData() {
		return playData;
	}
	/**
	 * @param playData the playData to set
	 */
	public void setPlayData(Map<String, Object> playData) {
		this.playData = playData;
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
	 * @return the joinUrl
	 */
	public String getJoinUrl() {
		return joinUrl;
	}
	/**
	 * @param joinUrl the joinUrl to set
	 */
	public void setJoinUrl(String joinUrl) {
		this.joinUrl = joinUrl;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GameJoinResponse [clientId=" + clientId + ", gameSlotId="
				+ gameSlotId + ", joinUrl=" + joinUrl + ", message=" + message
				+ ", nickname=" + nickname + ", playData=" + playData
				+ ", playTime=" + playTime + ", playUrl=" + playUrl
				+ ", status=" + status + ", time=" + time + ", type=" + type
				+ ", version=" + version + "]";
	}
}
