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

/**
 * @author cmg
 *
 */
public enum GameJoinResponseStatus {
	OK,
	TRY_LATER, // play but not yet time (or server/instance not yet active)
	ERROR_FULL, // game is full
	ERROR_UNSUPPORTED_CLIENT, 
	ERROR_UNKNOWN_SLOT,
	ERROR_CLIENT_AUTHENTICATION_REQUIRED,
	ERROR_USER_AUTHENTICATION_REQUIRED, // not anonymous
	ERROR_AUTHENTICATION_FAILED,
	ERROR_NOT_PERMITTED,
	ERROR_INTERNAL,
	ERROR_BLOCKED, // slot/client explicitly blocked
	ERROR_CANCELLED, // game cancelled
	ERROR_ENDED // game cancelled
}
