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

/** Game time options from GameInstanceFactory match.
 * 
 * @author cmg
 *
 */
public class GameTimeOptions {
	/** one option field */
	private GameTimeOption second;
	/** one option field */
	private GameTimeOption minute;
	/** one option field */
	private GameTimeOption hour;
	/** one option field - count from 1 */
	private GameTimeOption dayOfMonth;
	/** one option field - count from 1*/
	private GameTimeOption month;
	/** one option field - count from 1=Sun  */
	private GameTimeOption dayOfWeek;
	/** one option field */
	private GameTimeOption year;
	/** cons */
	public GameTimeOptions() {}
	/**
	 * @return the second
	 */
	public GameTimeOption getSecond() {
		return second;
	}
	/**
	 * @param second the second to set
	 */
	public void setSecond(GameTimeOption second) {
		this.second = second;
	}
	/**
	 * @return the minute
	 */
	public GameTimeOption getMinute() {
		return minute;
	}
	/**
	 * @param minute the minute to set
	 */
	public void setMinute(GameTimeOption minute) {
		this.minute = minute;
	}
	/**
	 * @return the hour
	 */
	public GameTimeOption getHour() {
		return hour;
	}
	/**
	 * @param hour the hour to set
	 */
	public void setHour(GameTimeOption hour) {
		this.hour = hour;
	}
	/**
	 * @return the dayOfMonth
	 */
	public GameTimeOption getDayOfMonth() {
		return dayOfMonth;
	}
	/**
	 * @param dayOfMonth the dayOfMonth to set
	 */
	public void setDayOfMonth(GameTimeOption dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}
	/**
	 * @return the monthOfYear
	 */
	public GameTimeOption getMonth() {
		return month;
	}
	/**
	 * @param monthOfYear the monthOfYear to set
	 */
	public void setMonth(GameTimeOption month) {
		this.month = month;
	}
	/**
	 * @return the dayOfWeek
	 */
	public GameTimeOption getDayOfWeek() {
		return dayOfWeek;
	}
	/**
	 * @param dayOfWeek the dayOfWeek to set
	 */
	public void setDayOfWeek(GameTimeOption dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	/**
	 * @return the year
	 */
	public GameTimeOption getYear() {
		return year;
	}
	/**
	 * @param year the year to set
	 */
	public void setYear(GameTimeOption year) {
		this.year = year;
	}
	
}
