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
<h2>It looks like you haven't configured your site yet.</h2>
<p>
  Hippo Essentials makes it easier to setup a Hippo project.</p>
<p>With Essentials, you can browse the Essentials
  feature library and install extra functionality in your new Hippo project.</p>
<p>To get some basic understanding about Essentials and how to use it, check the
  <a href="http://www.onehippo.org/library/essentials/hippo-developer-essentials.html">documentation</a>.</p>
</p>
<h3>
  <a href="http://<%=request.getServerName() +':' + request.getServerPort()+"/essentials"%>">Start Hippo Essentials</a>
</h3>
</body>
</html>