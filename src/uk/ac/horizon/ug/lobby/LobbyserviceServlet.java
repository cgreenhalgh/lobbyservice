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

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.*;

@SuppressWarnings("serial")
public class LobbyserviceServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(LobbyserviceServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		logger.info("Get: contextPath="+req.getContextPath()+", pathInfo="+req.getPathInfo()+", queryString="+req.getQueryString()+", requestURI="+req.getRequestURI()+", localName="+req.getLocalName()+", localPort="+req.getLocalPort()+", serverName="+req.getServerName()+", serverPort="+req.getServerPort()+", serverPath="+req.getServletPath());
		
		if ("/json".equals(req.getPathInfo())) {
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("application/x-horizon-ug-lobby-json");
			resp.getWriter().println("{}");
			return;
		}
		
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello, world");
	}
}
