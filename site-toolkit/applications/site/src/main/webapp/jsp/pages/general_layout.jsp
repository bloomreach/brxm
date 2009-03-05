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
System.out.println("Console out from general_layout.jsp");
%>

<html>
<head>

<!-- include header -->
<hst:container name="header" />

<hst:response-properties var="responseProperties" />
<c:forEach var="propertyEntry" items="${responseProperties}">
  <c:set var="responseProperty" value="${propertyEntry.value}" />
  <script language="<x:out select="$responseProperty/@language" />" src="<x:out select="$responseProperty/@src" />">
    <x:out select="$responseProperty" escapeXml="false"/>
  </script>
</c:forEach>

</head>
<body>

<script language="javascript">
<!--
function <hst:namespace/>showPopup() {
    alert("Hello from general_layout  component!");
}
//-->
</script>

<span class="title">The new Hst is Coooooooooooool</span>

<div class="page">
    <div>
    
	    <a href="javascript:<hst:namespace/>showPopup();">Show</a>
	    
	    <hst:renderURL var="homeUrl">
	      <hst:param name="page" />
	    </hst:renderURL>
	    <hst:renderURL var="firstUrl">
	      <hst:param name="page" value="1" />
	    </hst:renderURL>
	    <hst:renderURL var="lastUrl">
	      <hst:param name="page" value="9" />
	    </hst:renderURL>
	
	    <a href="${homeUrl}">Home</a>
	    <a href="${firstUrl}">First</a>
	    <a href="${lastUrl}">Last</a>
    
    </div>
    
    <hst:container name="body" />
    
    <div>
        news parameters: <%=request.getParameterMap()%>
    </div>

</div>

</body>
</html>