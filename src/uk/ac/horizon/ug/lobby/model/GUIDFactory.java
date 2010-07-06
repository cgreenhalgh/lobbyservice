/**
 * 
 */
package uk.ac.horizon.ug.lobby.model;

import java.util.UUID;

/**
 * @author cmg
 *
 */
public class GUIDFactory {
	/** get a GUID (actually a UUID at the mo) */
	private static synchronized String newGUID() {
		return UUID.randomUUID().toString();
	}
}
