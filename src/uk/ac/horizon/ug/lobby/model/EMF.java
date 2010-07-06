/**
 * 
 */
package uk.ac.horizon.ug.lobby.model;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author cmg
 *
 */
public class EMF {
    private static final EntityManagerFactory emfInstance = 
        Persistence.createEntityManagerFactory("transactions-optional"); 
 
    private EMF() {} 
 
    public static EntityManagerFactory get() { 
        return emfInstance; 
    } 

}
