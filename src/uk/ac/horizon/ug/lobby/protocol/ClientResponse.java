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

import java.util.List;

/**
 * @author cmg
 *
 */
public class ClientResponse {
	/** protocol/object version */
	private int version;
	/** time - response generated */
	private Long time;
	/** status */
	private ClientResponseStatus status;
	/** message resposne */
	private String message;
	/** game instances (with extra info) - for GAME_LIST */
	private List<GameTemplateInfo> games;
	/** cons */
	public ClientResponse() {}
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
	 * @return the status
	 */
	public ClientResponseStatus getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(ClientResponseStatus status) {
		this.status = status;
	}
	/**
	 * @return the games
	 */
	public List<GameTemplateInfo> getGames() {
		return games;
	}
	/**
	 * @param games the games to set
	 */
	public void setGames(List<GameTemplateInfo> games) {
		this.games = games;
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
	
}
