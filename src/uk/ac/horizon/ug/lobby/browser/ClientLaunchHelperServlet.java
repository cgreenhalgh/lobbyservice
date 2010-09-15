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
package uk.ac.horizon.ug.lobby.browser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author cmg
 *
 */
public class ClientLaunchHelperServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(ClientLaunchHelperServlet.class.getName());

	static String DEFAULT_MIME_TYPE = "application/x-uk.ac.horizon.ug.lobby";
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String mimeType = req.getParameter("mimeType");
		if (mimeType==null || mimeType.length()==0)
			mimeType = DEFAULT_MIME_TYPE;
		logger.info("doGet("+mimeType+")");
		resp.setContentType(mimeType);
		resp.setCharacterEncoding("UTF-8");
		PrintWriter pw = new PrintWriter(resp.getWriter());
		pw.print("{}");
		pw.close();
	}

}
