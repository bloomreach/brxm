<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<% response.setStatus(403); %>

<fmt:setBundle basename="org.hippoecm.hst.security.servlet.LoginServlet" />
<html lang="en">
  <head>
    <meta charset="utf-8"/>
    <title><fmt:message key="label.access.forbidden" /></title>
    <link rel="stylesheet" type="text/css" href="<hst:link path='/login/hst/security/skin/screen.css' />" />
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