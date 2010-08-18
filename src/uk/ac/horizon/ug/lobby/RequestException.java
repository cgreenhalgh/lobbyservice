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
package uk.ac.horizon.ug.lobby;

/**
 * @author cmg
 *
 */
public class RequestException extends Exception {
	private int errorCode;
	/**
	 * 
	 */
	public RequestException(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @param arg0
	 */
	public RequestException(int errorCode, String arg0) {
		super(arg0);
		this.errorCode = errorCode;
	}

	/**
	 * @param arg0
	 */
	public RequestException(int errorCode, Throwable arg0) {
		super(arg0);
		this.errorCode = errorCode;
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RequestException(int errorCode, String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.errorCode = errorCode;
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+" ("+errorCode+")";
	}

}
