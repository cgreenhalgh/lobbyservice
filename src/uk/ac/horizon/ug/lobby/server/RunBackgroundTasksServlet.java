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
package uk.ac.horizon.ug.lobby.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;

/** run periodic background tasks */
@SuppressWarnings("serial")
public class RunBackgroundTasksServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(RunBackgroundTasksServlet.class.getName());
	
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}


	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		logger.info("RunBackgroundTasksServlet");
		try {
			FactoryTasks.checkAllGameInstanceFactories();
		} 
		catch (Exception e) {
			logger.log(Level.WARNING,"Error doing checkAllGameInstanceFactories", e);
		}
			
		try {
			GameInstanceTasks.checkAllGameInstances();
		} 
		catch (Exception e) {
			logger.log(Level.WARNING,"Error doing checkAllGameInstances", e);
		}
			
		resp.setContentType("text/plain");
		resp.getWriter().close();
	}
}
