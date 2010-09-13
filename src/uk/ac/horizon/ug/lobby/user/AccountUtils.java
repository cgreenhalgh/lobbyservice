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
package uk.ac.horizon.ug.lobby.user;

import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;

/**
 * @author cmg
 *
 */
public class AccountUtils {
	static Logger logger = Logger.getLogger(AccountUtils.class.getName());
	/** get Account for current user, creating if required */
	public static Account getAccount(HttpServletRequest req) throws RequestException {
        UserService userService = UserServiceFactory.getUserService(); 
        
        if (req!=null && req.getUserPrincipal() == null) { 
        	logger.warning("getUserPrinciple failed");
        	throw new RequestException(HttpServletResponse.SC_UNAUTHORIZED, "User is not authenticated");
        }
        User user = userService.getCurrentUser();
        if (user==null) {
        	logger.warning("getCurrentUser failed");
        	throw new RequestException(HttpServletResponse.SC_UNAUTHORIZED, "User is not authenticated");
        }
        
        // Something stinks here...
        // when i run this code twice concurrently as first op on desktop deployment it
        // throws java.lang.UnsupportedOperationException
        //    at org.datanucleus.store.appengine.EntityUtils.getPropertyName(EntityUtils.java:62)
        //    ...
        // this appears to be an 'old' error with datanucleus initialisation:
        //    http://groups.google.com/group/google-appengine-java/browse_thread/thread/ba7f6868ffbebbc9/930c3f3b313863e0?lnk=gst&q=UnsupportedOperationException+#930c3f3b313863e0
        // but in that case why is it still happening now?!
        synchronized (Account.class) {
        	EntityManager em = EMF.get().createEntityManager();
        	Account account = null;
        	EntityTransaction et = em.getTransaction();
        	et.begin();
        	try {
        		// Hmm. So (on GAE) any query without an ancestor constraint cannot be part of a transaction.
        		// So assuming that we don't want to put all Accounts into the same EntityGroup (for performance)
        		// we probably just have to make sure that our 'unique' name is actually used in the key,
        		// so we are not really querying at all.
        		//Query q = em.createQuery("SELECT x FROM "+Account.class.getName()+" x WHERE x.userId = :userId");
        		//q.setParameter("userId", user.getUserId());
        		// on testing userId = null; fall back to email?!
        		String userId = user.getUserId();
        		if (userId==null)
        		{
        			userId = user.getEmail();
        			logger.warning("getAccount falling back to email for "+userId+" ("+user+")");
        		}
        		Key accountKey = Account.userIdToKey(userId);

        		// in transaction
        		account = em.find(Account.class, accountKey);
        		if (account==null) {
        			logger.info("Creating new Account for "+userId+": email="+user.getEmail()+", nickname="+user.getNickname());
        			account = new Account();
        			account.setKey(Account.userIdToKey(userId));
        			account.setUserId(userId);
        			account.setNickname(user.getNickname());
        			// can't create by default
        			account.setGameTemplateQuota(0);
        			em.persist(account);
        			et.commit();
        		}
        	}
        	finally {
        		if (et.isActive())
        			et.rollback();
        		em.close();
        	}
        	return account;
        }
	}
}
