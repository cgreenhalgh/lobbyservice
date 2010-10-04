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
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.ug.lobby.model.GameClient;
import uk.ac.horizon.ug.lobby.user.ClientManagementServlet;

import com.google.appengine.api.datastore.*;

/**
 * @author cmg
 *
 */
public class UpdateSchemaServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(UpdateSchemaServlet.class.getName());

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.info("Updating...");
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query(GameClient.class.getSimpleName());
		//q.addFilter("createdTime", Query.FilterOperator.EQUAL, null);
		for (Entity gce : datastore.prepare(q).asIterable()) {
			if (gce.getProperty("createdTime")==null) {
				gce.setProperty("createdTime", System.currentTimeMillis());
				datastore.put(gce);
				logger.info("Set createdTime on GameClient "+gce.getProperty("clientId"));
			}
			else {
				logger.info("Leave GameClient "+gce.getProperty("clientId")+" with createdTime="+gce.getProperty("createdTime"));
			}
		}
		logger.info("Done");
	}

}
