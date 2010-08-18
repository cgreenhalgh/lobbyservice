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

import uk.ac.horizon.ug.lobby.model.GameClientTemplate;
import uk.ac.horizon.ug.lobby.model.GameTemplate;

/**
 * @author cmg
 *
 */
public class GameTemplateInfo {
	/** game template */
	private GameTemplate gameTemplate;
	/** client templates */
	private List<GameClientTemplate> gameClientTemplates;
	/** cons */
	public GameTemplateInfo() {		
	}
	/**
	 * @return the gameTemplate
	 */
	public GameTemplate getGameTemplate() {
		return gameTemplate;
	}
	/**
	 * @param gameTemplate the gameTemplate to set
	 */
	public void setGameTemplate(GameTemplate gameTemplate) {
		this.gameTemplate = gameTemplate;
	}
	/**
	 * @return the gameClientTemplates
	 */
	public List<GameClientTemplate> getGameClientTemplates() {
		return gameClientTemplates;
	}
	/**
	 * @param gameClientTemplates the gameClientTemplates to set
	 */
	public void setGameClientTemplates(List<GameClientTemplate> gameClientTemplates) {
		this.gameClientTemplates = gameClientTemplates;
	}
	
}
