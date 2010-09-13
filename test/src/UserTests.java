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
import uk.ac.horizon.ug.lobby.model.GameInstance;
import uk.ac.horizon.ug.lobby.model.GameServer;
import uk.ac.horizon.ug.lobby.model.GameTemplate;
import uk.ac.horizon.ug.lobby.model.ServerConfiguration;
import uk.ac.horizon.ug.lobby.protocol.GameTemplateInfo;
import uk.ac.horizon.ug.lobby.protocol.JSONUtils;
import uk.ac.horizon.ug.lobby.user.AccountUtils;
import uk.ac.horizon.ug.lobby.user.AddGameInstanceServlet;
import uk.ac.horizon.ug.lobby.user.AddGameServerServlet;
import uk.ac.horizon.ug.lobby.user.AddGameTemplateServlet;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
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

public class UserTests {
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

	// try adding a GameTemplate
	static String TEST_TEMPLATE = "{'id':'165acda5-8f63-4dd1-a883-cb39a61f1d94', "+
	"'title':'Exploding Places', "+
	"'description':'A game of virtual communities, played over 100 years of local history.',"+
	"'link':'http://www.explodingplaces.org/', "+
	"'imageUrl':'http://89.200.142.192/favicon.ico', "+
	"'visibility':'PUBLIC', "+
	"'clientTemplates':["+
	  "{'title':'test client', 'clientType':'ANDROID', 'minMajorVersion':1, 'minMinorVersion':6, 'minUpdateVersion':0, 'locationSpecific':true, "+
	  "'applicationLaunchId':'uk.ac.horizon.ug.exploding.client.LOBBY_LAUNCH', 'applicationMarketId':'uk.ac.horizon.ug.exploding.client'}"+
	"]}";
	
	static String TEST_TEMPLATE2 = "{"+
	"'title':'Exploding Places2', "+
	"'description':'A game of virtual communities, played over 100 years of local history.',"+
	"'link':'http://www.explodingplaces.org/', "+
	"'imageUrl':'http://89.200.142.192/favicon.ico', "+
	"'visibility':'PUBLIC', "+
	"'clientTemplates':["+
	  "{'title':'test client', 'clientType':'ANDROID', 'minMajorVersion':1, 'minMinorVersion':6, 'minUpdateVersion':0, 'locationSpecific':true, "+
	  "'applicationLaunchId':'uk.ac.horizon.ug.exploding.client.LOBBY_LAUNCH', 'applicationMarketId':'uk.ac.horizon.ug.exploding.client'}"+
	"]}";
	
	@Test
	public void cantAddTemplateWithNoQuota() throws RequestException, JSONException {
		try {
			new AdminTests().setConfig();

			Account account = AccountUtils.getAccount(null);
			assertNotNull("Account not created", account);
			GameTemplateInfo gameTemplateInfo = JSONUtils.parseGameTemplateInfo(new JSONObject(TEST_TEMPLATE));
			AddGameTemplateServlet.testHandleAddGameTemplate(gameTemplateInfo, account);
			Assert.fail("Add template succeeded without quota");
		}
		catch (RequestException re) {
			assertEquals("Wrong error code", HttpServletResponse.SC_FORBIDDEN, re.getErrorCode());
		}
	}

	@Test
	public void addTemplate() throws RequestException, JSONException {
		new AdminTests().setAccountTemplateQuota();

		Account account = AccountUtils.getAccount(null);
		GameTemplateInfo gameTemplateInfo = JSONUtils.parseGameTemplateInfo(new JSONObject(TEST_TEMPLATE));
		AddGameTemplateServlet.testHandleAddGameTemplate(gameTemplateInfo, account);
		String id = gameTemplateInfo.getGameTemplate().getId();
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameTemplate gt = em.find(GameTemplate.class, GameTemplate.idToKey(id));
			assertNotNull("GameTemplate not found", gt);
			assertEquals("GameTemplate title", "Exploding Places", gt.getTitle());
		}
		finally {
			em.close();
		}
	}
	
	@Test
	public void cantAddTemplateOverQuota() throws RequestException, JSONException {
		addTemplate();
		try {
			Account account = AccountUtils.getAccount(null);
			GameTemplateInfo gameTemplateInfo = JSONUtils.parseGameTemplateInfo(new JSONObject(TEST_TEMPLATE2));
			AddGameTemplateServlet.testHandleAddGameTemplate(gameTemplateInfo, account);
			Assert.fail("Add template succeeded without quota");
		}
		catch (RequestException re) {
			assertEquals("Wrong error code", HttpServletResponse.SC_FORBIDDEN, re.getErrorCode());
		}
	}

	// try getting an index (no templates)
	@Test
	public void getIndex0() throws RequestException, JSONException {
		// TODO
	}
	// try getting an index (one template)

	// try adding a GameServer for the above GameTemplate
	static String TEST_SERVER = "{'baseUrl':'http://localhost:8080/exploding',"+
	"'gameTemplateId':'165acda5-8f63-4dd1-a883-cb39a61f1d94',"+
	//"'key':'ahBjaHJpcy1ncmVlbmhhbGdochALEgpHYW1lU2VydmVyGAIM',"+
	"'lastKnownStatus':'UNKNOWN','title':'server1','lastKnownStatusTime':1282222568004,"+
	"'lobbySharedSecret':'1234','targetStatus':'UP','type':'EXPLODING_PLACES'}";
	
	@Test
	public void addServerTest() throws RequestException, JSONException {
		addTemplate();
		addServer();
	}
	
	Key addServer() throws RequestException, JSONException {
		Account account = AccountUtils.getAccount(null);
		GameServer gameServer = JSONUtils.parseGameServer(new JSONObject(TEST_SERVER));
		AddGameServerServlet.testHandleAddGameServer(gameServer, account);
		Key key = gameServer.getKey();
		EntityManager em = EMF.get().createEntityManager();
		try {
			GameServer gs = em.find(GameServer.class, key);
			assertNotNull("GameServer not found", gs);
			assertEquals("GameServer baseUrl", "http://localhost:8080/exploding", gs.getBaseUrl());
		}
		finally {
			em.close();
		}
		return key;
	}
	
	// try running background tasks on GameInstanceFactory to create GameInstances (not server lifecycle)
	
	// try adding a GameInstance

	// try adding a GameInstanceFactory
	static String TEST_INSTANCE = "{'baseUrl':'http://localhost:8080/',"+
	//"endTime":1282226400000,
	//"'gameServerId":"ahBjaHJpcy1ncmVlbmhhbGdochALEgpHYW1lU2VydmVyGAIM",
	"'gameTemplateId':'165acda5-8f63-4dd1-a883-cb39a61f1d94',"+
	//"key":"ahBjaHJpcy1ncmVlbmhhbGdochILEgxHYW1lSW5zdGFuY2UYAww",
	//"latitudeE6":0,"longitudeE6":0,
	"'nominalStatus':'PLANNED',"+
	//"radiusMetres":0,
	//"startTime":1282222800000,
	//"'title':"game1"
	"}";
	
	static JSONObject getTestInstanceJson(long startTime, long endTime, String title, Key gameServerKey) throws JSONException {
		JSONObject o = new JSONObject(TEST_INSTANCE);
		o.put("startTime", startTime);
		o.put("endTime", endTime);
		if(title!=null)
			o.put("title", title);
		if (gameServerKey!=null)
			o.put("gameServerId", KeyFactory.keyToString(gameServerKey));
		return o;
	}

	@Test
	public void addGameInstance() throws RequestException, JSONException {
		addTemplate();
		Key gameServerKey = addServer();
		long now = System.currentTimeMillis();
		Account account = AccountUtils.getAccount(null);
		JSONObject json = getTestInstanceJson(now, now+60*60*1000, "inst1", gameServerKey);
		GameInstance gi = JSONUtils.parseGameInstance(json);
		AddGameInstanceServlet.testHandleAddGameInstance(gi, account);
	}
	
	// try query with some GameInstances
	
	// try location-constrained query
	
	// try time-constrained query
	
	// try newinstance
	
	// try reserve
	
	// try play

	// (try release)
	
	// try list games for client
	
	// try list games for account
	
	// TODO 
}
