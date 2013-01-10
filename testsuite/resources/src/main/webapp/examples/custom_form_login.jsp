<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%--
NOTE: This page is provided as an example for a customized login page for a mount.
--%>

<%@ page language="java" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.hippoecm.hst.security.servlet.LoginServlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
String username = StringUtils.defaultString(request.getParameter(LoginServlet.USERNAME));
String destination = StringUtils.defaultString(request.getParameter(LoginServlet.DESTINATION));

HttpSession httpSession = request.getSession(false);

if (httpSession != null) {
    if (StringUtils.isBlank(username)) {
        username = StringUtils.defaultString((String) httpSession.getAttribute(LoginServlet.USERNAME_ATTR_NAME));
    }
    
    if (StringUtils.isBlank(destination)) {
        destination = StringUtils.defaultIfEmpty((String) httpSession.getAttribute(LoginServlet.DESTINATION_ATTR_NAME), request.getContextPath() + "/");
    }
}

pageContext.setAttribute("j_username", username);
pageContext.setAttribute("destination", response.encodeURL(destination));
%>

<html>
  <head>
    <title>Login</title>
    <link rel="stylesheet" type="text/css" href="../login/hst/security/skin/screen.css" />
  </head>
  <body class="hippo-root" onload="return document.signInForm.username.focus();">
    <div>
      <div class="hippo-login-panel">
        <form class="hippo-login-panel-form" name="signInForm" method="post" action="../login/proxy">
          <h2><div class="hippo-global-hideme"><span>Hippo CMS 7</span></div></h2>
          <div class="hippo-login-form-container">
            <table>
              <tr>
                <td width="50%"><label>User name&nbsp;</label></td>
                <td><input class="hippo-form-text" type="text" value="${j_username}" name="username" id="username"/></td>
              </tr>
              <tr>
                <td><label>Password&nbsp;</label></td>
                <td><input class="hippo-form-password" type="password" value="" name="password" id="password"/></td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td class="hippo-global-alignright">
                  <input type="hidden" name="destination" value="${destination}" />
                  <input class="hippo-form-submit" type="submit" value="Log in"/>
                  <input class="hippo-form-submit" type="button" value="Cancel" onclick="if ('${destination}') location.href = '${destination}'; return false;" />
                </td>
              </tr>
            </table>
          </div>
        </form>
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2011 Hippo B.V.
        </div>
      </div>
    </div>
  </body>
</html>