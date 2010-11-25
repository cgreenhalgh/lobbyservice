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

/** Request from (new) client to set shared secret. Should only be send over HTTPS/SSL.
 * Mostly fields from GameClient
 * 
 * @author cmg
 *
 */
public class RegisterClientRequest {
	/** protocol/object version */
	private int version;
	/** version constant */
	public static final int VERSION = 1;
	/** time - optional for duplicate request/replay detection */
	private Long time;
	/** sequence - optional for duplicate request/replay detection */
	private Integer seqNo;
	/** client Id */
	private String clientId;
	/** shared secret */
	private String sharedSecret;
    /** default nickname */
    private String nickname;
    /** client characteristics - JSON encoded object, i.e. set of property names & values */
    private String characteristicsJson;
    /** cache of parsed characteristics */
    private transient JSONObject characteristics;
    // now characteristic OSName
    /** client type, e.g. "Android" */
    //private String clientType;
    // now characteristic OSVersion
    /** min major version */
    //private Integer majorVersion;
    /** min minor version (or 0) */
    //private Integer minorVersion;
    /** min update (or 0) */
    //private Integer updateVersion;
	/** cons */
	public RegisterClientRequest() {		
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
	 * @return the sharedSecret
	 */
	public String getSharedSecret() {
		return sharedSecret;
	}
	/**
	 * @param sharedSecret the sharedSecret to set
	 */
	public void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
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
}
