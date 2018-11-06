<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%@ page isErrorPage="true" %>
<% response.setStatus(404); %>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <title>404 error</title>
</head>
<body>
<h2>Welcome to BloomReach</h2>
<p>
  It appears that you just created an empty BloomReach project from the archetype. There is nothing to show on the site yet.
  We recommend you use the
  <a href="http://<%=request.getServerName() + ':' + request.getServerPort() + "/essentials"%>" target="_blank">BloomReach setup application</a>
  to start developing your project.
</p>
</body>
</html>