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
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameTemplate;

/**
 * @author cmg
 *
 */
public class GameTemplateInfo {
	/** game template */
	private GameTemplate gameTemplate;
	/** game instance - optional - used in responses to GameQuerys */
	private GameInstance gameInstance;
	/** game instance factory - optional - used in responses to GameQuerys */
	private GameInstanceFactory gameInstanceFactory;
	/** first game instance start time - optional, with GameInstanceFactory */
	private Long firstStartTime;
	/** game time options - for GameInstanceFactory, based on startTimeCron */
	private GameTimeOptions gameTimeOptions;
	/** client templates */
	private List<GameClientTemplate> gameClientTemplates;
	/** game (template)-specific URL for lobby client interaction - query API (Template) */
	private String queryUrl;
	/** game (factory)-specific URL for lobby client interaction - new instance API (Factory) */
	private String newInstanceUrl;
	/** game (instance)-specific URL for lobby client interaction - reserve/join (Instance) */
	private String joinUrl;
	/** include 'private' fields */
	private boolean includePrivateFields;
	/** game slot id - for game instances with RESERVED slot (account/client-specific) */
	private String gameSlotId; 
	/** client id - assocated with game slot id - for game instances with RESERVED slot (account/client-specific) */
	private String clientId;
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
	/**
	 * @return the queryUrl
	 */
	public String getQueryUrl() {
		return queryUrl;
	}
	/**
	 * @param queryUrl the queryUrl to set
	 */
	public void setQueryUrl(String queryUrl) {
		this.queryUrl = queryUrl;
	}
	/**
	 * @return the gameInstance
	 */
	public GameInstance getGameInstance() {
		return gameInstance;
	}
	/**
	 * @param gameInstance the gameInstance to set
	 */
	public void setGameInstance(GameInstance gameInstance) {
		this.gameInstance = gameInstance;
	}
	/**
	 * @return the joinUrl
	 */
	public String getJoinUrl() {
		return joinUrl;
	}
	/**
	 * @param joinUrl the joinUrl to set
	 */
	public void setJoinUrl(String joinUrl) {
		this.joinUrl = joinUrl;
	}
	/**
	 * @return the gameInstanceFactory
	 */
	public GameInstanceFactory getGameInstanceFactory() {
		return gameInstanceFactory;
	}
	/**
	 * @param gameInstanceFactory the gameInstanceFactory to set
	 */
	public void setGameInstanceFactory(GameInstanceFactory gameInstanceFactory) {
		this.gameInstanceFactory = gameInstanceFactory;
	}
	/**
	 * @return the firstStartTime
	 */
	public Long getFirstStartTime() {
		return firstStartTime;
	}
	/**
	 * @param firstStartTime the firstStartTime to set
	 */
	public void setFirstStartTime(Long firstStartTime) {
		this.firstStartTime = firstStartTime;
	}
	/**
	 * @return the gameTimeOptions
	 */
	public GameTimeOptions getGameTimeOptions() {
		return gameTimeOptions;
	}
	/**
	 * @param gameTimeOptions the gameTimeOptions to set
	 */
	public void setGameTimeOptions(GameTimeOptions gameTimeOptions) {
		this.gameTimeOptions = gameTimeOptions;
	}
	/**
	 * @return the newInstanceUrl
	 */
	public String getNewInstanceUrl() {
		return newInstanceUrl;
	}
	/**
	 * @param newInstanceUrl the newInstanceUrl to set
	 */
	public void setNewInstanceUrl(String newInstanceUrl) {
		this.newInstanceUrl = newInstanceUrl;
	}
	/**
	 * @return the includePrivateFields
	 */
	public boolean isIncludePrivateFields() {
		return includePrivateFields;
	}
	/**
	 * @param includePrivateFields the includePrivateFields to set
	 */
	public void setIncludePrivateFields(boolean includePrivateFields) {
		this.includePrivateFields = includePrivateFields;
	}
	/**
	 * @return the gameSlotId
	 */
	public String getGameSlotId() {
		return gameSlotId;
	}
	/**
	 * @param gameSlotId the gameSlotId to set
	 */
	public void setGameSlotId(String gameSlotId) {
		this.gameSlotId = gameSlotId;
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
	
}
