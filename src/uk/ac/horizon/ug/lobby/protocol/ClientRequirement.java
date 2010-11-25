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

/** Requirement that must be satisfied by a Client (in terms of its Characteristics) in 
 * order to join a game. Normally stored JSON-encoded
 * 
 * @author cmg
 *
 */
public class ClientRequirement {
	/** characteristic which requirements applies to; typically one of ClientCharacteristicType */
	private String characteristic;
	/** simple boolean expression constraining characteristic's value.
	 * E.g. '=VALUE', '>=VALUE', '<=VALUE', 
	 * 'IN(VALUE,VALUE,...)', 'LIKE([%]VALUE[%])' (SQL inexact match, % is wildcard),
	 * 'TRUE','FALSE','UNDEFINED'
	 */
	private String expression;
	/** what if we fail the constraint test? */
	private ClientRequirementFailureType failure;
	/** sucess message (optional, probably for debug/diagnostics) */
	private String successMsg;
	/** Failure message */
	private String failureMsg;
	/** Failure URL - URL to be offered to user as possible contingency action */
	private String failureUrl;

	/** cons */
	public ClientRequirement() {		
	}

	/**
	 * @return the characteristic
	 */
	public String getCharacteristic() {
		return characteristic;
	}

	/**
	 * @param characteristic the characteristic to set
	 */
	public void setCharacteristic(String characteristic) {
		this.characteristic = characteristic;
	}

	/**
	 * @return the expression
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * @param expression the expression to set
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * @return the failure
	 */
	public ClientRequirementFailureType getFailure() {
		return failure;
	}

	/**
	 * @param failure the failure to set
	 */
	public void setFailure(ClientRequirementFailureType failure) {
		this.failure = failure;
	}

	/**
	 * @return the successMsg
	 */
	public String getSuccessMsg() {
		return successMsg;
	}

	/**
	 * @param successMsg the successMsg to set
	 */
	public void setSuccessMsg(String successMsg) {
		this.successMsg = successMsg;
	}

	/**
	 * @return the failureMsg
	 */
	public String getFailureMsg() {
		return failureMsg;
	}

	/**
	 * @param failureMsg the failureMsg to set
	 */
	public void setFailureMsg(String failureMsg) {
		this.failureMsg = failureMsg;
	}

	/**
	 * @return the failureUrl
	 */
	public String getFailureUrl() {
		return failureUrl;
	}

	/**
	 * @param failureUrl the failureUrl to set
	 */
	public void setFailureUrl(String failureUrl) {
		this.failureUrl = failureUrl;
	}

}
