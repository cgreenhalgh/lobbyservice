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

/** A possible (kind of) characteristic of a client, which may affect its ability to
 * participate in a game.
 * 
 * @author cmg
 *
 */
public enum ClientCharacteristicType {
	OSName, // Operating System Name, e.g. Android
	OSVersion, // Operating System version, e.g. 1.6
	AppID, // installed application ID, e.g. Android application package
	AppVersion, // installed application version
//	WebBrowser, // Presence of a web browser
//	HtmlVersion, // required version of HTML supported
	LocationGPS, // GPS (or other 'fine grained') positioning
	HtmlGeolocation, // support for geolocation API
//	HtmlJavascript, // Javascript enabled
//	NetPermitted, // Allowed to use network comms during game
//	NetBandwidth, // typical (estimated) available bandwidth, e.g. based on use of GPRS vs 3G vs Wifi
}
