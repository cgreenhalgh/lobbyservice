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
	ERROR_ENDED, // game cancelled
	ERROR_SCHEDULED_ONLY, // NEW_INSTANCE request made to SCHEDULED GameInstanceFactory
	ERROR_SYSTEM_QUOTA_EXCEEDED, // NEW_INSTANCE request, but Factory has no quota left
//	ERROR_USER_QUOTA_EXCEEDED // NEW_INSTANCE request, but no Client/Account quota left - not implemented
	ERROR_START_TIME_TOO_SOON, // NEW_INSTANCE request, start time too soon (too close to now)
	ERROR_START_TIME_INVALID, // NEW_INSTANCE request, start time not one supported by factory (e.g. out of range)
}
