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

/** Request from a client, initially to list GameInstances which it has slots for 
 * (with optional Query-like constraints).
 * 
 * @author cmg
 *
 */
public class ClientRequest {
	/** protocol/object version */
	private int version;
	/** time - optional for duplicate request/replay detection */
	private Long time;
	/** sequence - optional for duplicate request/replay detection */
	private Integer seqNo;
	/** client ID */
	private String clientId;
	/** request type */
	private ClientRequestType type;
	/** request scope (account or just this client) - default CLIENT */
	private ClientRequestScope scope;
	/** GAME_LIST: include planned (future) - default false */
	private Boolean includePlanned;
	/** GAME_LIST: include available (current) - default false*/
	private Boolean includeAvailable;
	/** GAME_LIST: include ended (past) - default false */
	private Boolean includeEnded;
	/** cons */
	public ClientRequest() {}
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
	 * @return the type
	 */
	public ClientRequestType getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(ClientRequestType type) {
		this.type = type;
	}
	/**
	 * @return the scope
	 */
	public ClientRequestScope getScope() {
		return scope;
	}
	/**
	 * @param scope the scope to set
	 */
	public void setScope(ClientRequestScope scope) {
		this.scope = scope;
	}
	/**
	 * @return the includePlanned
	 */
	public Boolean getIncludePlanned() {
		return includePlanned;
	}
	/**
	 * @param includePlanned the includePlanned to set
	 */
	public void setIncludePlanned(Boolean includePlanned) {
		this.includePlanned = includePlanned;
	}
	/**
	 * @return the includeAvailable
	 */
	public Boolean getIncludeAvailable() {
		return includeAvailable;
	}
	/**
	 * @param includeAvailable the includeAvailable to set
	 */
	public void setIncludeAvailable(Boolean includeAvailable) {
		this.includeAvailable = includeAvailable;
	}
	/**
	 * @return the includeEnded
	 */
	public Boolean getIncludeEnded() {
		return includeEnded;
	}
	/**
	 * @param includeEnded the includeEnded to set
	 */
	public void setIncludeEnded(Boolean includeEnded) {
		this.includeEnded = includeEnded;
	}
	
}
