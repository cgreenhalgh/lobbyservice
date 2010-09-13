import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.horizon.ug.lobby.ConfigurationUtils;
import uk.ac.horizon.ug.lobby.RequestException;
import uk.ac.horizon.ug.lobby.admin.AdminGameIndexServlet;
import uk.ac.horizon.ug.lobby.admin.UpdateAccountServlet;
import uk.ac.horizon.ug.lobby.model.Account;
import uk.ac.horizon.ug.lobby.model.EMF;
import uk.ac.horizon.ug.lobby.model.GUIDFactory;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.user.AccountUtils;
import uk.ac.horizon.ug.lobby.user.AddGameTemplateServlet;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig; 
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig; 
import com.google.appengine.tools.development.testing.LocalServiceTestHelper; 

import org.json.JSONObject;
import org.json.JSONException;

import static org.junit.Assert.*;

public class AdminTests {
	static final String TEST_USER_ID = "test@abc.def";
	private final LocalServiceTestHelper helper =     
		new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig(),
				new LocalUserServiceTestConfig())
				.setEnvEmail(TEST_USER_ID).setEnvAuthDomain("abc.def").setEnvIsAdmin(true).setEnvIsLoggedIn(true); 

	@Before     
	public void setUp() {      
		helper.setUp();  
	}      
	@After     
	public void tearDown() {   
		helper.tearDown();    
	} 

	@Test 
	public void setConfig() throws JSONException {
		String config = "{'description':'my description','title':'my title'}";
		JSONObject json = new JSONObject(config);
		AdminGameIndexServlet.testHandlePost(json);
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		Assert.assertEquals("description not set in ServerConfiguration", "my description", sc.getGameIndex().getDescription());
		Assert.assertEquals("docs not set in ServerConfiguration", "http://github.com/cgreenhalgh/lobbyservice", sc.getGameIndex().getDocs());
		Assert.assertEquals("title not set in ServerConfiguration", "my title", sc.getGameIndex().getTitle());
	}

	@Test 
	public void checkConfig() throws JSONException {
		// resets each test!
		setConfig();
		ServerConfiguration sc = ConfigurationUtils.getServerConfiguration();
		Assert.assertEquals("title not set in ServerConfiguration", "my title", sc.getGameIndex().getTitle());
	}

	@Test
	public void createAccountOnAccess() throws RequestException {
        //UserService userService = UserServiceFactory.getUserService(); 
        //User user = userService.getCurrentUser();
        //System.out.println("User: "+user+", "+user.getUserId()+", "+user.getNickname());
		Account account = AccountUtils.getAccount(null);
		assertNotNull("Account not created", account);
		assertEquals("Account id incorrect", TEST_USER_ID, account.getKey().getName());
		assertEquals("Account userId incorrect", TEST_USER_ID, account.getUserId());
	}
	
	@Test
	public void setAccountTemplateQuota() throws RequestException, JSONException {
		setConfig();
		Account account = AccountUtils.getAccount(null);
		assertNotNull("Account not created", account);
		JSONObject json = new JSONObject("{'userId':'"+TEST_USER_ID+"','gameTemplateQuota':1}");
		UpdateAccountServlet.testUpdateAccount(TEST_USER_ID, json);
		// NB it is for our own account
		account = AccountUtils.getAccount(null);
		assertEquals("Account gameTemplateQuota not update", 1, account.getGameTemplateQuota());
	}
}
