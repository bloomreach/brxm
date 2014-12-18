<%--
  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="tag" tagdir="/WEB-INF/tags" %>

<%@ attribute name="siteMenuItem" type="org.hippoecm.hst.core.sitemenu.HstSiteMenuItem" rtexprvalue="true" required="true"%>

<c:choose>
  <c:when test="${siteMenuItem.selected}">
    <b><c:out value="${siteMenuItem.name}"/></b>
  </c:when>
  <c:otherwise>
    <c:set var="link">
      <c:choose>
        <c:when test="${not empty siteMenuItem.externalLink}">${siteMenuItem.externalLink}</c:when>
        <c:otherwise><hst:link link="${siteMenuItem.hstLink}"/></c:otherwise>
      </c:choose>
    </c:set>
    <a href="${link}"><c:out value="${siteMenuItem.name}"/></a>
  </c:otherwise>
</c:choose>
<c:if test="${siteMenuItem.expanded and not empty siteMenuItem.childMenuItems}">
  <ul>
    <c:forEach var="child" items="${siteMenuItem.childMenuItems}">
      <li>
        <tag:menuitem siteMenuItem="${child}"/>
      </li>
    </c:forEach>
  </ul>
</c:if>
