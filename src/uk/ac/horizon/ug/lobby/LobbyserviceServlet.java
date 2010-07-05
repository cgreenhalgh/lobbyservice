package uk.ac.horizon.ug.lobby;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.*;

@SuppressWarnings("serial")
public class LobbyserviceServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(LobbyserviceServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		logger.info("Get: contextPath="+req.getContextPath()+", pathInfo="+req.getPathInfo()+", queryString="+req.getQueryString());
		
		if ("/json".equals(req.getPathInfo())) {
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("application/x-horizon-ug-lobby-json");
			resp.getWriter().println("{}");
			return;
		}
		
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello, world");
	}
}
