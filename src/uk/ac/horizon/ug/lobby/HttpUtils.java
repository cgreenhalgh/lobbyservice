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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author cmg
 *
 */
public class HttpUtils {
	/** get path within servlet context, e.g. instance id */
	public static String getIdFromPath(HttpServletRequest req) throws RequestException {
		String id = req.getPathInfo();
		if (id!=null && id.startsWith("/"))
			id = id.substring(1);
		if (id==null || id.length()==0)
			throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "No ID specified in path");
		return id;
	}
}
