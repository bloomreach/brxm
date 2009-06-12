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
<%@ page language="java" import="javax.jcr.*, javax.naming.*" %>

<html>
<head>
<title>JNDI Example for Pooling JCR Repository</title>
</head>
<body>

<h1>JNDI Example for Pooling JCR Repository</h1>
<hr>

<pre>
<%
Repository repository = null;

try {
	Context initCtx = new InitialContext();
	Context envCtx = (Context) initCtx.lookup("java:comp/env");
	repository = (Repository) envCtx.lookup("jcr/repository");
} catch (Exception e) {
    out.println("Failed to look up the pooling JCR repository: " + e);
}

if (repository != null) {
    Session jcrSession = null;
    
    try {
		Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
		jcrSession = repository.login(creds);
		out.println("Successfully logged in to the repository!");
    } catch (Exception e) {
        out.println("Failed to login: " + e);
    } finally {
        if (jcrSession != null) try { jcrSession.logout(); } catch (Exception ce) { }
    }
}
%>
</pre>

</html>