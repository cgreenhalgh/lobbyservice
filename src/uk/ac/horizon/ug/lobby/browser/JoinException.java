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
package uk.ac.horizon.ug.lobby.browser;

import uk.ac.horizon.ug.lobby.protocol.GameJoinResponse;
import uk.ac.horizon.ug.lobby.protocol.GameJoinResponseStatus;

/** GameJoin exception (specifically).
 * 
 * @author cmg
 *
 */
public class JoinException extends Exception {
	/** GameJoinResponseStatus */
	private GameJoinResponseStatus status;

	/**
	 * 
	 */
	public JoinException(GameJoinResponseStatus errorStatus, String message) {
		super(message);
		this.status = errorStatus;
	}

	/**
	 * 
	 */
	public JoinException(GameJoinResponseStatus errorStatus, String message, Throwable cause) {
		super(message, cause);
		this.status = errorStatus;
	}


	/**
	 * @return the status
	 */
	public GameJoinResponseStatus getStatus() {
		return status;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+" (Status="+getStatus()+")";
	}

}
