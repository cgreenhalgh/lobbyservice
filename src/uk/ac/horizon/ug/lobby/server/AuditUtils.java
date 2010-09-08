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

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.model.AuditRecordLevel;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameTemplateAuditRecord;
import uk.ac.horizon.ug.lobby.model.GameTemplateAuditRecordType;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/**
 * @author cmg
 *
 */
public class AuditUtils {
	static Logger logger = Logger.getLogger(AuditUtils.class.getName());
	public static void persist(Object o) {
		EntityManager em = EMF.get().createEntityManager();
		em.persist(o);
		em.close();
	}
	public static void logGameTemplateAuditRecord(String gameTemplateId,
			Key gameInstanceFactoryKey, Key gameInstanceKey, Key accountKey,
			String clientIp, long time, GameTemplateAuditRecordType type,
			AuditRecordLevel level, String detailsJson, String message) {
		GameTemplateAuditRecord r = new GameTemplateAuditRecord(null, gameTemplateId,
				gameInstanceFactoryKey, gameInstanceKey, accountKey,
				clientIp, time, type, level, detailsJson, message);
		persist(r);
		StringWriter sw = new StringWriter();
		JSONWriter jw = new JSONWriter(sw);
		try {
			JSONUtils.writeGameTemplateAuditRecord(jw, r);
			logger.info("logGameTemplateAuditRecord:"+sw.toString());
		} catch (JSONException e) {
			logger.log(Level.WARNING,"Writing GameTemplateAuditRecord", e);
		}
	}
}
