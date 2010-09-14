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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.google.appengine.api.datastore.Key;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.Constants;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameInstanceFactory;
import uk.ac.horizon.ug.lobby.model.GameInstanceNominalStatus;
import uk.ac.horizon.ug.lobby.model.GameInstanceStatus;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameServerStatus;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;

/** GameInstance (background) tasks.
 * 
 * @author cmg
 *
 */
public class GameInstanceTasks implements Constants {
	static Logger logger = Logger.getLogger(FactoryUtils.class.getName());
	/** check all GameInstances - periodic task */
	public static void checkAllGameInstances() {
		EntityManager em = EMF.get().createEntityManager();
		try {
			// TODO narrow down set of GameInstances needing checking?
			Query q = em.createQuery("SELECT x FROM GameInstance x ORDER BY x."+START_TIME);
			List<GameInstance> gis = (List<GameInstance>)q.getResultList();
			for (GameInstance gi : gis) {
				try {
					checkGameInstance(gi);
				}
				catch (Exception e) {
					logger.log(Level.WARNING,"doing checkGameInstance("+gi+")", e);
				}
			}
		}
		finally {
			em.close();
		}
	}
	/** check one GameInstance - periodic.
	 * (watch out for races if called from elsewhere/concurrently) */
	private static void checkGameInstance(GameInstance gi) {
		// don't fiddle with UNMANAGED instances
		if (gi.getStatus()==GameInstanceStatus.UNMANAGED || gi.getStatus()==null)
			return;

		// GameInstanceFactory (currently) creates new instances with nominalStatus 'PLANNED'.

		// Query returns GameInstances with nominalStatus IN ( 'PLANNED', 'POSSIBLE', 'AVAILABLE', 'TEMPORARILY_UNAVAILABLE' )
		// i.e. not CANCELLED or ENDED

		// Join returns TRY_LATER for instances with nominalStatus 'PLANNED', 'POSSIBLE', 'TEMPORARILY_UNAVAILABLE'
		//      returns ERROR_CANCELLED/_ENDED for instances with nominalStatus 'CANCELLED' or 'ENDED'
		//      returns ERROR_ENDED for instances with nominalStatus AVAILABLE after endTime
		// Join, for instances which are AVAILABLE, before endTime...
		//      returns TRY_LAYER for servers with targetStatus not 'UP' (i.e. UNKNOWN, DOES_NOT_EXIST, STOPPED, DOWN, ERROR)
		//      else calls serverProtocol.handlePlayRequest 
			
		GameInstanceFactory factory = null;
		GameServer server = null;

		EntityManager em = EMF.get().createEntityManager();
		try {
			if (gi.getGameInstanceFactoryKey()!=null) {
				factory = em.find(GameInstanceFactory.class, gi.getGameInstanceFactoryKey());
				if (factory==null) {
					// TODO audit?
					logger.warning("Could not find Factory "+gi.getGameInstanceFactoryKey()+" for instance "+gi.getKey());
					return;
				}
			}
			// can't (won't) do anything with non-factory instances (not enough information)
			if (factory==null)
				return;

			Key serverKey = gi.getGameServerId();
			if (serverKey==null && factory!=null)
				serverKey = factory.getGameServerId();
			if (serverKey==null) {
				logger.warning("No Server specified for instance "+gi.getKey()+"(or factory "+gi.getGameInstanceFactoryKey()+")");
				// TODO audit?
				return;
			}
			server = em.find(GameServer.class, serverKey);
			if (server==null) {
				logger.warning("Could not find Server "+serverKey+" for instance "+gi.getKey()+"(or factory "+gi.getGameInstanceFactoryKey()+")");
				// TODO audit?
				return;
			}
		}
		finally {
			em.close();
		}
			
		// Factory tells us server(Create,Start,Ending,End)TimeOffsetMs.
		//         also serverConfigJson
		// Server tells us baseUrl if not already in Instance (Factory doesn't put it there)

		// GameInstance status is our view of the 'real' (external) game instance status.

		// Do we need to change the instance's nominalStatus? 
		long now = System.currentTimeMillis();
		GameInstanceNominalStatus targetNominalStatus = gi.getNominalStatus();
		switch(targetNominalStatus) {
		case AVAILABLE:
			if (now>=gi.getEndTime())
				// should have ended now
				targetNominalStatus = GameInstanceNominalStatus.ENDED;
			break;
		case CANCELLED:
			// no op?!
			break;
		case ENDED:
			// no op
			break;
		case PLANNED:
		case TEMPORARILY_UNAVAILABLE:
			if (now>=gi.getEndTime())
				// should have ended now
				targetNominalStatus = GameInstanceNominalStatus.ENDED;
			else if (now>=gi.getStartTime() || now>=gi.getStartTime()+factory.getServerCreateTimeOffsetMs())
				// should be available now
				targetNominalStatus = GameInstanceNominalStatus.AVAILABLE;
			break;
		}			

		// given that target nominal status, what 'real' status would we expect at the moment?
		GameInstanceStatus targetStatus = null;
		switch (targetNominalStatus) {
		case PLANNED:
		case AVAILABLE:
		case ENDED:
		case TEMPORARILY_UNAVAILABLE: // never actually a target status!
			if (now<gi.getStartTime()+factory.getServerCreateTimeOffsetMs())
				// not yet time
				targetStatus = GameInstanceStatus.PLANNED;
			else if (now<gi.getStartTime()+factory.getServerStartTimeOffsetMs())
				// not yet start
				targetStatus = GameInstanceStatus.READY;
			else if (now<gi.getEndTime()+factory.getServerEndingTimeOffsetMs())
				// not yet ending
				targetStatus = GameInstanceStatus.ACTIVE;
			else if (now<gi.getEndTime()+factory.getServerEndTimeOffsetMs())
				// not yet end
				targetStatus = GameInstanceStatus.ENDING;
			else
				targetStatus = GameInstanceStatus.ENDED;
			break;
		case CANCELLED:
			if (gi.getStatus()==GameInstanceStatus.PLANNED || gi.getStatus()==GameInstanceStatus.CANCELLED)
				targetStatus = GameInstanceStatus.CANCELLED;
			else
				// it has already started in some way, so the best we can do is end it
				targetStatus = GameInstanceStatus.ENDED;
			break;				
		}

		// some basic consistency checking...
		if (targetNominalStatus==GameInstanceNominalStatus.AVAILABLE && (targetStatus!=GameInstanceStatus.READY && targetStatus!=GameInstanceStatus.ACTIVE && targetStatus!=GameInstanceStatus.ENDING)) {
			logger.warning("GameInstance(Factory) configuration problem: targetNominalStatus is "+targetNominalStatus+" but targetStatus="+targetStatus+" (not joinable): "+factory);
		}
		else if (targetNominalStatus==GameInstanceNominalStatus.ENDED && (targetStatus!=GameInstanceStatus.ENDING && targetStatus!=GameInstanceStatus.ENDED)) {
			logger.warning("GameInstance(Factory) configuration problem: targetNominalStatus is "+targetNominalStatus+" but targetStatus="+targetStatus+" (not ending): "+factory);
		}

		try {
			logger.info("NominalStatus="+gi.getNominalStatus()+", target="+targetNominalStatus+", status="+gi.getStatus()+", target="+targetStatus);
			// lets try to achieve these target states...
			switch (gi.getStatus()) {
			case ACTIVE:
				switch (targetStatus) {
				case ACTIVE:
					// no-op
					break;
				case ENDED:
				case CANCELLED:
					doEndFromPreparing(gi, factory, server);
					break;
				case ENDING:
					doEndingFromActive(gi, factory, server);
					break;
				default: // PLANNED, READY
					// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
			case CANCELLED:
				switch (targetStatus) {
				case ENDED:
				case CANCELLED:
					// no op
					break;
				default: // PLANNED, READY, ACTIVE
					// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
			case ENDED:
				switch (targetStatus) {
				case ENDED:
				case CANCELLED:
					// no op
					break;
				default: // PLANNED, READY, ACTIVE
					// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
			case ENDING:
				switch (targetStatus) {
				case ENDING:
					// no-op
					break;
				case ENDED:
				case CANCELLED:
					doEndFromPreparing(gi, factory, server);
					break;
				default: // ACTIVE, PLANNED, READY
					// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
				//case ERROR:
				//			case FAILED:
				//			case PAUSED:
			case PLANNED:
				switch (targetStatus) {
				case PLANNED:
					// no-op
					break;
				case ENDED:
				case CANCELLED:
					doCancelFromPlanned(gi, factory, server);
					break;
				case READY:
					doPreparingFromPlanned(gi, factory, server);
					doReadyFromPreparing(gi, factory, server);
					break;
				case ACTIVE:
					doPreparingFromPlanned(gi, factory, server);
					doReadyFromPreparing(gi, factory, server);
					doActiveFromReady(gi, factory, server);
					break;
				case ENDING:
					doPreparingFromPlanned(gi, factory, server);
					doReadyFromPreparing(gi, factory, server);
					doActiveFromReady(gi, factory, server);
					doEndingFromActive(gi, factory, server);
					break;
				default: // ? 
						// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
				//			case POSSIBLE:
			case PREPARING: // assume game exists but not yet ready
				switch (targetStatus) {
				case ENDED:
				case CANCELLED:
					doEndFromPreparing(gi, factory, server);
					break;
				case READY:
					doReadyFromPreparing(gi, factory, server);
					break;
				case ACTIVE:
					doReadyFromPreparing(gi, factory, server);
					doActiveFromReady(gi, factory, server);
					break;
				case ENDING:
					doReadyFromPreparing(gi, factory, server);
					doActiveFromReady(gi, factory, server);
					doEndingFromActive(gi, factory, server);
					break;
				default: // ? 
						// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
			case READY:
				switch (targetStatus) {
				case ENDED:
				case CANCELLED:
					doEndFromPreparing(gi, factory, server);
					break;
				case READY:
					// no op
					break;
				case ACTIVE:
					doActiveFromReady(gi, factory, server);
					break;
				case ENDING:
					doActiveFromReady(gi, factory, server);
					doEndingFromActive(gi, factory, server);
					break;
				default: // ? 
					// just leave it
					logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				}
				break;
				//			case STOPPED:
			case UNMANAGED:
				// shouldn't be here anyway!
				logger.warning("desired instance status change "+gi.getStatus()+" -> "+targetStatus+" not possible: "+gi);
				break;
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error managing game instance ", e);
		}

		em = EMF.get().createEntityManager();
		EntityTransaction et= em.getTransaction();
		// transaction!
		et.begin();
		try {
			// check / update nominal status
			GameInstance ngi = em.find(GameInstance.class, gi.getKey());
			if (ngi.getStatus()==targetStatus || 
					// at the instance level ENDED and CANCELLED are essentially the same outcome (no more game)
					(ngi.getStatus()==GameInstanceStatus.CANCELLED && targetStatus==GameInstanceStatus.ENDED) ||
					(ngi.getStatus()==GameInstanceStatus.ENDED && targetStatus==GameInstanceStatus.CANCELLED)) {
				// met target so presumably met nominal target
				ngi.setNominalStatus(targetNominalStatus);
				//em.merge(ngi);
				et.commit();
				logger.info("GameInstance reached targetStatus="+targetStatus+"; updating nominalStatus to "+targetNominalStatus);
			}
			else {
				// didn't meet target, so...
				// AVAILABLE is the only one with teeth!
				if (targetNominalStatus==GameInstanceNominalStatus.AVAILABLE && ngi.getNominalStatus()==GameInstanceNominalStatus.AVAILABLE && (ngi.getStatus()!=GameInstanceStatus.READY && ngi.getStatus()!=GameInstanceStatus.ACTIVE && ngi.getStatus()!=GameInstanceStatus.ENDING)) {
					logger.warning("Failed to reach AVAILABLE status: "+ngi.getStatus()+"; marking "+gi.getKey()+" as TEMPORARILY_UNAVAILABLE");
					ngi.setNominalStatus(GameInstanceNominalStatus.TEMPORARILY_UNAVAILABLE);
					//em.merge(ngi);
					et.commit();
				}
				else
					logger.warning("Failed to reach target status "+targetStatus+": "+ngi.getStatus()+"; leaving nominal status ("+ngi.getNominalStatus()+")");
			}
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();			
		}
	}
	private static void updateStatus(GameInstance gi, GameInstanceStatus oldStatus, GameInstanceStatus newStatus) {
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction et = em.getTransaction();
		try {
			et.begin();
			GameInstance ngi = em.find(GameInstance.class, gi.getKey());
			if (oldStatus!=null && ngi.getStatus()!=oldStatus) 
				throw new RuntimeException("updateStatus found status "+ngi.getStatus()+" vs "+oldStatus+" - refused");
			ngi.setStatus(newStatus);
			//em.merge(ngi);
			et.commit();
		}
		finally {
			if (et.isActive())
				et.rollback();
			em.close();
		}
		// fiddle with cache too
		gi.setStatus(newStatus);
	}
	private static ServerProtocol getServerProtocol(GameServer server) {
		if (server.getTargetStatus()!=GameServerStatus.UP) 
			throw new RuntimeException("GameServer "+server.getTitle()+" is not intended to be up ("+server.getTargetStatus()+")");
		ServerProtocol serverProtocol = server.getType().serverProtocol();
		return serverProtocol;
	}
	private static void doCancelFromPlanned(GameInstance gi,
			GameInstanceFactory factory, GameServer server) {
		updateStatus(gi, GameInstanceStatus.PLANNED, GameInstanceStatus.CANCELLED);
	}
	private static void doPreparingFromPlanned(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		getServerProtocol(server).handleGameInstancePreparingFromPlanned(gi, factory, server);
		
		updateStatus(gi, GameInstanceStatus.PLANNED, GameInstanceStatus.PREPARING);
	}
	private static void doReadyFromPreparing(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		getServerProtocol(server).handleGameInstanceReadyFromPreparing(gi, factory, server);

		updateStatus(gi, GameInstanceStatus.PREPARING, GameInstanceStatus.READY);	
	}
	private static void doActiveFromReady(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		getServerProtocol(server).handleGameInstanceActiveFromReady(gi, factory, server);
		
		updateStatus(gi, GameInstanceStatus.READY, GameInstanceStatus.ACTIVE);
	}
	private static void doEndingFromActive(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		getServerProtocol(server).handleGameInstanceEndingFromActive(gi, factory, server);
		
		updateStatus(gi, GameInstanceStatus.ACTIVE, GameInstanceStatus.ENDING);
	}
	private static void doEndFromPreparing(GameInstance gi,
			GameInstanceFactory factory, GameServer server) throws ConfigurationException, IOException {
		// Note: this can also be called from READY, ACTIVE and ENDING
		getServerProtocol(server).handleGameInstanceEnd(gi, factory, server);
		
		updateStatus(gi, null, GameInstanceStatus.ENDED);
	}
}
