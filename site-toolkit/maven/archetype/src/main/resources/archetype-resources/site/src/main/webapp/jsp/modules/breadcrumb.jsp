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
  limitations under the License.
--%>
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="${package}.core.template.*, org.hippoecm.hst.core.template.node.*,javax.jcr.*,java.util.*" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst-template" prefix="hst-tmpl"%>
  
<hst-tmpl:module name="breadcrumb" var="result" className="org.hippoecm.hst.components.modules.breadcrumb.RepositoryBasedBreadcrumbModule" execute="false" render="true"/>

<div id="breadcrumb">
<hst-tmpl:link var="homeLink" location="homepage"/>
 
<a href="${dollar}{homeLink.href}" title="Home">Home</a> &#62;
<c:forEach var="item" items="${dollar}{result}" varStatus="resultStatus">
  <c:choose>
    <c:when test="${dollar}{resultStatus.last == true}">
      <span>${dollar}{item.decodedName}</span>
    </c:when>
    <c:otherwise>
      <hst-tmpl:link var="link" item="${dollar}{item}"/>
      <a href="${dollar}{link.href}" title="${dollar}{item.decodedName}">${dollar}{item.decodedName}</a> &#62;
    </c:otherwise>
  </c:choose>   
</c:forEach>

<hr class="hidden" />
<!--// breadcrumb -->
