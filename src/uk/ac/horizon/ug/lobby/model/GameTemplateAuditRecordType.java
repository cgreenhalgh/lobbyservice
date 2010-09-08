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
public enum GameTemplateAuditRecordType {
	USER_CREATE_GAME_TEMPLATE,
	USER_UPDATE_GAME_TEMPLATE,
	USER_CREATE_GAME_INSTANCE_FACTORY,
	USER_UPDATE_GAME_INSTANCE_FACTORY,
	INVALID_GAME_INSTANCE_FACTORY,
	USER_CREATE_GAME_INSTANCE,
	USER_UPDATE_GAME_INSTANCE,
	SYSTEM_CREATE_GAME_INSTANCE,
	SYSTEM_CREATE_GAME_INSTANCE_FAILED
}
