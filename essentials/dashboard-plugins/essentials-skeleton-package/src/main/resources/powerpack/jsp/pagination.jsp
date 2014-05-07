<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

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

<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<ul class="pagination">
  <li class="disabled"><a href="#">${pageable.total} document(s)</a></li>
  <c:forEach var="pageNr" items="${pageable.pageNumbersArray}" varStatus="index">
    <hst:renderURL var="pageUrl">
      <hst:param name="page" value="${pageNr}"/>
      <hst:param name="pageSize" value="${pageable.pageSize}"/>
    </hst:renderURL>
    <c:if test="${index.first and pageable.previous}">
      <hst:renderURL var="pageUrlPrevious">
        <hst:param name="page" value="${pageNr}"/>
        <hst:param name="pageSize" value="${pageable.pageSize}"/>
      </hst:renderURL>
      <li><a href="${pageUrlPrevious}">previous</a></li>
    </c:if>
    <c:choose>
      <c:when test="${pageable.currentPage eq pageNr}">
        <li class="active"><a href="#">${pageNr}</a></li>
      </c:when>
      <c:otherwise>
        <li><a href="${pageUrl}">${pageNr}</a></li>
      </c:otherwise>
    </c:choose>
    <c:if test="${index.last and pageable.next}">
      <hst:renderURL var="pageUrlNext">
        <hst:param name="page" value="${pageNr}"/>
        <hst:param name="pageSize" value="${pageable.pageSize}"/>
      </hst:renderURL>
      <li><a href="${pageUrlNext}">next</a></li>
    </c:if>
  </c:forEach>
</ul>
