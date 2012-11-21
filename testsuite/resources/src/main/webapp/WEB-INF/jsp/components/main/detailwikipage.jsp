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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>


<hst:headContribution keyHint="title"><title><c:out value="${document.title}" /></title></hst:headContribution>
<hst:element name="script" var="yui3Elem">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="http://yui.yahooapis.com/3.2.0/build/yui/yui-min.js" />
</hst:element>
<hst:headContribution keyHint="yui3" element="${yui3Elem}" />

<div id="<hst:namespace/>detailPane" class="yui-u">
  <h1><c:out value="${document.title}" /></h1>
  
  <p>
    City : 
    <c:out value="${document.placetime.place.city}" />
  </p>
  <p>
    Country : 
    <c:out value="${document.placetime.place.country}" />
  </p>
  <p>
    Date :
    <fmt:formatDate value="${document.placetime.date.time}" pattern="MMM dd, yyyy"/>
  </p>
  <p>
    Categories :<br />
    <c:forEach var="category" items="${document.categories}">
      <c:out value="${category}" /><br />
    </c:forEach>
  </p>
  <p>
    Related documents :<br />
    <c:forEach var="reldoc" items="${document.relatedDocs}">
      <hst:link var="link" hippobean="${reldoc}" canonical="true" />
	  <c:choose>
        <c:when test="${empty reldoc.title}">
	      <a href="${link}">${reldoc.name}</a>
		</c:when>
		<c:otherwise>
		  <a href="${link}">${reldoc.title}</a>
		</c:otherwise>
      </c:choose>
	  <br />
	</c:forEach>
  <p>
    Content : 
    <c:forEach var="block" items="${document.blocks}">
	  <c:if test="${not empty block.header}">
        <h2><c:out value="${block.header}" /></h2>
      </c:if>
      
	  <c:if test="${not empty block.image}">
	    <img src="<hst:link hippobean="${block.image.original}"/>"/>
	  </c:if>
  
	  <c:if test="${not empty block.body}">
	    <p><hst:html hippohtml="${block.body}" /></p>
      </c:if>
	</c:forEach>
  </p>
 </div>