import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameTemplate;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig; 
import com.google.appengine.tools.development.testing.LocalServiceTestHelper; 

public class JPATest {
	private final LocalServiceTestHelper helper =     
		new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()); 

	@Before     
	public void setUp() {      
		helper.setUp();  
	}      
	@After     
	public void tearDown() {   
		helper.tearDown();    
	} 

	    @Test 
	public void simpleGet() {
		EntityManager em = EMF.get().createEntityManager();
		String id = GUIDFactory.newGUID();
		try {
			GameTemplate gt = new GameTemplate();
			gt.setId(id);
			gt.setTitle("Test");
			em.persist(gt);
		}
		finally {
			em.close();
		}
		em = EMF.get().createEntityManager();
		try {
			Key key = GameTemplate.idToKey(id);
			GameTemplate gt = em.find(GameTemplate.class, key);
			Assert.assertNotNull(gt);
			Assert.assertEquals(gt.getTitle(), "Test");
			System.out.println("GameTemplate "+gt.getTitle()+" = "+gt);
		}
		finally {
			em.close();
		}
	}
}
