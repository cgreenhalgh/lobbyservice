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

import javax.persistence.Embeddable;

/** SNMP-style experiment - embeddable counter.
 * 
 * @author cmg
 *
 */
@Embeddable
public class AuditCounter {
	/** current value */
	private int value;
	/** (java) time started/resit */
	private long startTime;
	/** (java) time updated */
	private long updateTime;
	/** cons */
	public AuditCounter() {}
	/** add count.
	 * return new value. */
	public int add(int delta) {
		value = value + delta;
		updateTime = System.currentTimeMillis();
		return value;
	}
	/** reset; return old value */
	public int clear() {
		int oldValue = value;
		value = 0;
		startTime = System.currentTimeMillis();
		return oldValue;
	}
	/**
	 * @return the value
	 */
	public int getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}
	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	/**
	 * @return the updateTime
	 */
	public long getUpdateTime() {
		return updateTime;
	}
	/**
	 * @param updateTime the updateTime to set
	 */
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
}
