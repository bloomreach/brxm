/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.components.modules.login;

import org.hippoecm.hst.core.template.module.ModuleBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * The Login module uses the execute() method to create an authenticated jcrSession that is set
 * on the HttpSession. Content related repository data is then accessed by this jcrSession.<br/>
 * The template related repository data is still accessed by the default jcrSession.
 * </p>
 *
 */
public class LoginModule extends ModuleBase {
	private static final Logger log = LoggerFactory.getLogger(LoginModule.class);
	
	/**
	 * This method verifies if a login form has been submitted. If that is the case it tries
	 * to create an authenticated jcrSession and puts it on the HttpSession.
	 */
//	public String execute(HttpServletRequest request, HttpServletResponse response)
//			throws TemplateException {
//				
//		
//		String login = request.getParameter("login");
//		if (login != null) {
//			String name = request.getParameter("name");
//			String password = request.getParameter("password");
//			log.info("execute with name=" + name + "password=" + password);
//			Session jcrSession = JCRConnectorWrapper.getJCRSession(request.getSession(), name, password);
//			
//			request.getSession().setAttribute(JCRConnectorWrapper.AUTHENTICATED_USER_SESSION_ATTRIBUTE, jcrSession);
//		}
//		
//		String logout = request.getParameter("logout");
//		if (logout != null) {
//			request.getSession().invalidate();
//		}
//		return null;
//	}

	/**
	 * Returns a loginBean with the user credentials if logged in. 
	 */
//	public void render(PageContext pageContext) {
//		LoginBean bean = new LoginBean();	
//		if (pageContext.getSession().getAttribute(JCRConnectorWrapper.AUTHENTICATED_USER_SESSION_ATTRIBUTE) == null) {
//		    bean.setLoggedIn(false);	
//		} else {
//			Session jcrSession = (Session) pageContext.getSession().getAttribute(JCRConnectorWrapper.AUTHENTICATED_USER_SESSION_ATTRIBUTE);
//			bean.setUserId(jcrSession.getUserID());
//		}
//		pageContext.setAttribute("loginBean", bean);
//	}

}
