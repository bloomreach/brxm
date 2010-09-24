<%--
  Copyright 2008-2009 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page language="java" %>
<%
String username = (String) session.getAttribute("org.hippoecm.hst.security.servlet.username");
if (username == null) username = "";
String password = (String) session.getAttribute("org.hippoecm.hst.security.servlet.password");
if (password == null) password = "";
%>
<html>
<body onload="return document.getElementById('loginForm').submit();">

<form id='loginForm' method='POST' action='<%=response.encodeURL("j_security_check")%>'>
  <input type='hidden' name='j_username' value='<%=username%>' /> 
  <input type='hidden' name='j_password' value='<%=password%>' />
  <noscript><input type='submit' value="Login now" /></noscript>
</form>

</body>
</html>