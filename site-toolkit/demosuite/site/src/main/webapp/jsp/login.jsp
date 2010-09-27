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
<%@ page import="org.hippoecm.hst.security.servlet.LoginServlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<%
String username = (String) session.getAttribute("org.hippoecm.hst.security.servlet.username");
if (username == null) username = "";
String password = (String) session.getAttribute("org.hippoecm.hst.security.servlet.password");
if (password == null) password = "";
String destination = request.getParameter("destination");
if (destination == null) destination = "";
%>

<html>
<head>
<title>Login page</title>
</head>
<body>

<div>
<h2>Login page</h2>
<form method="post" action="<c:url value="/login/proxy"/>">
<table>
  <tr>
    <td>Login</td>
    <td><input type="text" name="username" value="<%=username%>" /></td>
  </tr>
  <tr>
    <td>Password</td>
    <td><input type="password" name="password" value="<%=password%>" /></td>
  </tr>
  <tr>
    <td colspan="2">
      <input type="hidden" name="destination" value="<%=destination%>" />
      <input type="submit" value="Submit" />
      <input type="button" value="Cancel" onclick="if ('<%=destination%>') location.href = '<%=destination%>'; return;" />
    </td>
  </tr>
</table>
</form>
</div>

</body>
</html>