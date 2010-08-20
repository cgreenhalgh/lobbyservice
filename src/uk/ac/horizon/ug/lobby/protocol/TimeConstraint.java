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

/** Query constraint on a single time.
 * 
 * @author cmg
 *
 */
public class TimeConstraint {
	/** min time (Java time - ms since 1/1/1970) - optional */
	private Long minTime;
	/** min time (Java time - ms since 1/1/1970) - optional */
	private Long maxTime;
	/** min duration (ms) - optional */
	private Long minDurationMs;
	/** max duration (ms) - optional */
	private Long maxDurationMs;
	/** include games already started - default false */
	private boolean includeStarted;
	/** check game end is before max time as well as start - default false */
	private boolean limitEndTime;
	/** cons */
	public TimeConstraint() {		
	}
	/**
	 * @return the minTime
	 */
	public Long getMinTime() {
		return minTime;
	}
	/**
	 * @param minTime the minTime to set
	 */
	public void setMinTime(Long minTime) {
		this.minTime = minTime;
	}
	/**
	 * @return the maxTime
	 */
	public Long getMaxTime() {
		return maxTime;
	}
	/**
	 * @param maxTime the maxTime to set
	 */
	public void setMaxTime(Long maxTime) {
		this.maxTime = maxTime;
	}
	/**
	 * @return the minDurationMs
	 */
	public Long getMinDurationMs() {
		return minDurationMs;
	}
	/**
	 * @param minDurationMs the minDurationMs to set
	 */
	public void setMinDurationMs(Long minDurationMs) {
		this.minDurationMs = minDurationMs;
	}
	/**
	 * @return the maxDurationMs
	 */
	public Long getMaxDurationMs() {
		return maxDurationMs;
	}
	/**
	 * @param maxDurationMs the maxDurationMs to set
	 */
	public void setMaxDurationMs(Long maxDurationMs) {
		this.maxDurationMs = maxDurationMs;
	}
	/**
	 * @return the includeStarted
	 */
	public boolean isIncludeStarted() {
		return includeStarted;
	}
	/**
	 * @param includeStarted the includeStarted to set
	 */
	public void setIncludeStarted(boolean includeStarted) {
		this.includeStarted = includeStarted;
	}
	/**
	 * @return the limitEndTime
	 */
	public boolean isLimitEndTime() {
		return limitEndTime;
	}
	/**
	 * @param limitEndTime the limitEndTime to set
	 */
	public void setLimitEndTime(boolean limitEndTime) {
		this.limitEndTime = limitEndTime;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TimeConstraint [includeStarted=" + includeStarted
				+ ", limitEndTime=" + limitEndTime + ", maxDurationMs="
				+ maxDurationMs + ", maxTime=" + maxTime + ", minDurationMs="
				+ minDurationMs + ", minTime=" + minTime + "]";
	}
	
}
