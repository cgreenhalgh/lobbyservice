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

import uk.ac.horizon.ug.lobby.model.GameClientStatus;

/** User/account update to client e.g. status.
 * 
 * @author cmg
 *
 */
public class ClientManagementRequest {
	/** version */
	private int version;
	/** current version */
	public static final int VERSION = 1;
	/** client id */
	private String clientId;
	/** status (target) */
	private GameClientStatus newStatus;
	/** client authentication timestamp - for requests made in browser at instigation of client */
	private String clientTime;
	/** client authentication hmac - for requests made in browser at instigation of client */
	private String clientHmac;
	/** cons */
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
	 * @return the newStatus
	 */
	public GameClientStatus getNewStatus() {
		return newStatus;
	}
	/**
	 * @param newStatus the newStatus to set
	 */
	public void setNewStatus(GameClientStatus newStatus) {
		this.newStatus = newStatus;
	}
	/**
	 * @return the clientTime
	 */
	public String getClientTime() {
		return clientTime;
	}
	/**
	 * @param clientTime the clientTime to set
	 */
	public void setClientTime(String clientTime) {
		this.clientTime = clientTime;
	}
	/**
	 * @return the clientHmac
	 */
	public String getClientHmac() {
		return clientHmac;
	}
	/**
	 * @param clientHmac the clientHmac to set
	 */
	public void setClientHmac(String clientHmac) {
		this.clientHmac = clientHmac;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ClientManagementRequest [clientHmac=" + clientHmac
				+ ", clientId=" + clientId + ", clientTime=" + clientTime
				+ ", newStatus=" + newStatus + ", version=" + version + "]";
	}
	
}
