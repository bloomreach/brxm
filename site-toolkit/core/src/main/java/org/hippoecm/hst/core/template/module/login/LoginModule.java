package org.hippoecm.hst.core.template.module.login;

import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginModule extends ModuleBase {
	private static final Logger log = LoggerFactory.getLogger(LoginModule.class);
	
	public String execute(HttpServletRequest request, HttpServletResponse response)
			throws TemplateException {
				
		
		String login = request.getParameter("login");
		if (login != null) {
			String name = request.getParameter("name");
			String password = request.getParameter("password");
			log.info("execute with name=" + name + "password=" + password);
			Session jcrSession = JCRConnectorWrapper.getJCRSession(request.getSession(), name, password);
			
			request.getSession().setAttribute(JCRConnectorWrapper.AUTHENTICATED_USER_SESSION_ATTRIBUTE, jcrSession);
		}
		
		String logout = request.getParameter("logout");
		if (logout != null) {
			request.getSession().invalidate();
		}
		return null;
	}

	public void init(HttpServletRequest request) {
	}

	public void render(PageContext pageContext) {
		LoginBean bean = new LoginBean();	
		if (pageContext.getSession().getAttribute(JCRConnectorWrapper.AUTHENTICATED_USER_SESSION_ATTRIBUTE) == null) {
		    bean.setLoggedIn(false);	
		} else {
			Session jcrSession = (Session) pageContext.getSession().getAttribute(JCRConnectorWrapper.AUTHENTICATED_USER_SESSION_ATTRIBUTE);
			bean.setUserId(jcrSession.getUserID());
		}
		pageContext.setAttribute("loginBean", bean);
	}

	public String getVariableName() {
		// TODO Auto-generated method stub
		return null;
	}	
}

