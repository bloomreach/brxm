<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<% response.setStatus(403); %>

<fmt:setBundle basename="org.hippoecm.hst.security.servlet.LoginServlet" />
<html>
  <head>
    <title>
         <fmt:message key="label.access.forbidden" />
    </title>
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