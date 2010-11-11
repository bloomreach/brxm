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
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
String destination = (String) session.getAttribute("org.hippoecm.hst.security.servlet.destination");
if (destination == null) destination = "";
%>
<html>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />
<hst:link var="loginPageUrl" path="/login/form">
   <hst:param name="destination" value="<%=destination%>" />
</hst:link>
<meta http-equiv='refresh' content='1;${loginPageUrl}' />
<title>Authentication Required</title>
</head>
<body>
<H2>Authentication Required.</H2>
<hr/>
<P>Page will be automatically redirected to the login page.</P>
</body>
</html>