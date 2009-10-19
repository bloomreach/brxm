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
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<%
System.out.println("Console out from webpage.jsp");
%>

<html>
<head>

childContentNames: 
<hst:defineObjects/>
<c:forEach var="childContentName" items="${hstResponse.childContentNames}">
  <hst:include ref="${childContentName}" />
</c:forEach>

<!-- include header -->
<hst:include ref="header" />

<hst:head-contributions />

</head>
<body>

<h1>The new Hst</h1>


<div class="page">
    <div style="float:left">
        <hst:include ref="leftmenu" />
    </div>
    <div style="float:left">
        <hst:include ref="body" />
    </div>
</div>

</body>
</html>
