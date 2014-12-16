<!doctype html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

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