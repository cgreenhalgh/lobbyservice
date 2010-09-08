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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.AuditRecordLevel;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameTemplateAuditRecord;
import uk.ac.horizon.ug.lobby.model.GameTemplateAuditRecordType;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/**
 * @author cmg
 *
 */
public class AuditUtils implements Constants {
	static Logger logger = Logger.getLogger(AuditUtils.class.getName());
	public static void persist(Object o) {
		EntityManager em = EMF.get().createEntityManager();
		em.persist(o);
		em.close();
	}
	/** create/persist a GameTemplateAuditRecord */
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
	/** create/persist a GameTemplateAuditRecord if the last AuditRecord for this
	 * instance / instancefactor / template is not of the same type.
	 * 
	 * @param gameTemplateId
	 * @param gameInstanceFactoryKey
	 * @param gameInstanceKey
	 * @param accountKey
	 * @param clientIp
	 * @param time
	 * @param type
	 * @param level
	 * @param detailsJson
	 * @param message
	 */
	public static void logGameTemplateAuditRecordIfNovel(String gameTemplateId,
			Key gameInstanceFactoryKey, Key gameInstanceKey, Key accountKey,
			String clientIp, long time, GameTemplateAuditRecordType type,
			AuditRecordLevel level, String detailsJson, String message) {
		EntityManager em = EMF.get().createEntityManager();
		Query q = null;
		if (gameInstanceKey!=null) {
			q = em.createQuery("SELECT x."+type+" FROM GameTemplateAuditRecord x WHERE x."+GAME_INSTANCE_KEY+" = :"+GAME_INSTANCE_KEY+" ORDER BY x."+TIME+" DESC");
			q.setParameter(GAME_INSTANCE_KEY, gameInstanceKey);
		} else if (gameInstanceFactoryKey!=null) {
			q = em.createQuery("SELECT x."+type+" FROM GameTemplateAuditRecord x WHERE x."+GAME_INSTANCE_FACTORY_KEY+" = :"+GAME_INSTANCE_FACTORY_KEY+" ORDER BY x."+TIME+" DESC");
			q.setParameter(GAME_INSTANCE_FACTORY_KEY, gameInstanceFactoryKey);
		} else if (gameTemplateId!=null) {
			q = em.createQuery("SELECT x."+type+" FROM GameTemplateAuditRecord x WHERE x."+GAME_TEMPLATE_ID+" = :"+GAME_TEMPLATE_ID+" ORDER BY x."+TIME+" DESC");
			q.setParameter(GAME_TEMPLATE_ID, gameTemplateId);
		} else if (accountKey!=null) {
			q = em.createQuery("SELECT x."+type+" FROM GameTemplateAuditRecord x WHERE x."+ACCOUNT_KEY+" = :"+ACCOUNT_KEY+" ORDER BY x."+TIME+" DESC");
			q.setParameter(ACCOUNT_KEY, accountKey);
		} else {
			logGameTemplateAuditRecord(gameTemplateId,
				gameInstanceFactoryKey, gameInstanceKey, accountKey,
				clientIp, time, type, level, detailsJson, message);
			return;
		}
		q.setMaxResults(1);
		List<GameTemplateAuditRecordType> types = (List<GameTemplateAuditRecordType>)q.getResultList();
		if (types.size()>0 && types.get(0)==type) {
			em.close();
			GameTemplateAuditRecord r = new GameTemplateAuditRecord(null, gameTemplateId,
					gameInstanceFactoryKey, gameInstanceKey, accountKey,
					clientIp, time, type, level, detailsJson, message);
			StringWriter sw = new StringWriter();
			JSONWriter jw = new JSONWriter(sw);
			try {
				JSONUtils.writeGameTemplateAuditRecord(jw, r);
				logger.info("logGameTemplateAuditRecordIfNovel - ignored:"+sw.toString());
			} catch (JSONException e) {
				logger.log(Level.WARNING,"Writing GameTemplateAuditRecord", e);
			}
			return;
		}
		em.close();
		logGameTemplateAuditRecord(gameTemplateId,
				gameInstanceFactoryKey, gameInstanceKey, accountKey,
				clientIp, time, type, level, detailsJson, message);
	}
}
