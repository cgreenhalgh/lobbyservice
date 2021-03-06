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
package uk.ac.horizon.ug.lobby.model;

/**
 * @author cmg
 *
 */
public enum GameInstanceStatus {
	UNMANAGED, // nothing to do with us!
//	POSSIBLE,
	PLANNED,
	CANCELLED, // i.e. never happened
	PREPARING, // create sub-state - players cannot yet join
	READY, // done create - players could now join
//	STARTING, // transition to ACTIVE
	ACTIVE, // done start - playing, normal, players can join
	ENDING, // done ending - playing, ending, players can join
	ENDED, // done end - stopped/ended, players cannot join
//	ARCHIVED,
//	ERROR, // temporary
//	FAILED, // permanent
//	STOPPING, // deliberate, transition to STOPPED
//	STOPPED, // not supported
//	PAUSING, // resumable, transition to PAUSED
//	PAUSED, // not supported
//	RESUMING; // transition to ACTIVE (from PAUSED)
}
