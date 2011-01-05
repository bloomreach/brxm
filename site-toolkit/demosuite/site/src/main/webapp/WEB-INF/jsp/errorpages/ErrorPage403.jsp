<%--
  Copyright 2008 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.hippoecm.hst.core.container.ContainerSecurityException" %>
<%@ page import="org.hippoecm.hst.core.container.ContainerSecurityNotAuthenticatedException" %>
<%@ page import="org.hippoecm.hst.core.container.ContainerSecurityNotAuthorizedException" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix="hst"%>

<fmt:setBundle basename="org.hippoecm.hst.security.servlet.LoginServlet" />

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
String destination = (String) session.getAttribute("org.hippoecm.hst.security.servlet.destination");
if (destination == null) destination = "";

String title = "Authentication Required";
String description = "Authentication Required: You need to sign in to access " + destination + " on this server";

ContainerSecurityException securityException = (ContainerSecurityException) session.getAttribute("org.hippoecm.hst.security.servlet.exception");

if (securityException instanceof ContainerSecurityNotAuthorizedException) {
    title = "Forbidden";
    description = "Forbidden: You don't have permission to access " + destination + " on this server.";
}

session.invalidate();
%>

<hst:link var="loginFormUrl" path="/login/form" mount="site">
  <hst:param name="destination" value="<%=destination%>" />
</hst:link>
<hst:link var="loginProxyUrl" path="/login/proxy" mount="site">
  <hst:param name="destination" value="<%=destination%>" />
</hst:link>

<html>
  <head>
    <title><%=title%></title>
    <meta http-equiv='refresh' content='3;url=${loginFormUrl}' />
    <link rel="stylesheet" type="text/css" href="<hst:link path='/login/hst/security/skin/screen.css' mount='site'/>" />
  </head>
  <body class="hippo-root">
    <div>
      <div class="hippo-login-panel">
        <form class="hippo-login-panel-form" name="signInForm" method="post" action="${loginProxyUrl}">
          <h2><div class="hippo-global-hideme"><span>Hippo CMS 7</span></div></h2>
          <div class="hippo-login-form-container">
            <table>
              <tr>
                <td>
                  <p><%=description%></p>
                </td>
              </tr>
              <tr>
                <td>
                  <p>
                    <a href="${loginFormUrl}"><fmt:message key="message.try.again"/></a>
                    <br/><br/>
                    <fmt:message key="message.page.auto.redirect.in.seconds">
                      <fmt:param value="3" />
                    </fmt:message>
                  </p>
                </td>
              </tr>
            </table>
          </div>
        </form>
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2010 Hippo B.V.
        </div>
      </div>
    </div>
  </body>
</html>