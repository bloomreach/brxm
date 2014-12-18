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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="pages" required="true" type="java.util.List" rtexprvalue="true" %>
<%@ attribute name="page" required="true" type="java.lang.Integer" rtexprvalue="true" %>
<%@ attribute name="query" required="false" type="java.lang.String" rtexprvalue="true" %>
<c:if test="${fn:length(pages) gt 0}">
  <ul id="paging-nav">
    <c:forEach var="page" items="${pages}">
      <c:set var="active" value=""/>
      <c:choose>
        <c:when test="${crPage == page}">
          <li>${page}</li>
        </c:when>
        <c:otherwise>
          <hst:renderURL var="pagelink">
            <hst:param name="page" value="${page}"/>
            <hst:param name="query" value="${query}"/>
          </hst:renderURL>
          <li><a href="${pagelink}" title="${page}">${page}</a></li>
        </c:otherwise>
      </c:choose>
    </c:forEach>
  </ul>
</c:if>
