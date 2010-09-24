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
String destination = request.getParameter(LoginServlet.DESTINATION);
if (destination == null) {
    destination = "";
}
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
    <td><input type="text" name="<%=LoginServlet.USERNAME%>"/></td>
  </tr>
  <tr>
    <td>Password</td>
    <td><input type="password" name="<%=LoginServlet.PASSWORD%>"/></td>
  </tr>
  <tr>
    <td colspan="2">
      <input type="hidden" name="destination" value="<%=destination%>" />
      <input type="submit" value="login"/>
    </td>
  </tr>
</table>
</form>
</div>

</body>
</html>