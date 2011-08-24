<%--
  Copyright 2008 - 2011 Hippo

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

<html>
  <head>
    <title>
         <fmt:message key="label.access.forbidden" />
    </title>
    <link rel="stylesheet" type="text/css" href="<hst:link path='/login/hst/security/skin/screen.css' mount='site'/>" />
  </head>
  <body class="hippo-root">
    <div>
      <div class="hippo-login-panel">
        <fmt:message key="label.access.forbidden" />
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2011 Hippo B.V.
        </div>
      </div>
    </div>
  </body>
</html>