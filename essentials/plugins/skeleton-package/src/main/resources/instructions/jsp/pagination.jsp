<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<hst:setBundle basename="essentials.pagination"/>
<ul class="pagination">
  <li class="disabled">
    <a href="#">${requestScope.pageable.total}&nbsp;<fmt:message key="results.indication" var="indication"/><c:out value="${indication}"/></a>
  </li>
  <c:if test="${requestScope.pageable.totalPages > 1}">
    <c:forEach var="pageNr" items="${requestScope.pageable.pageNumbersArray}" varStatus="index">
      <hst:renderURL var="pageUrl">
        <hst:param name="page" value="${pageNr}"/>
        <hst:param name="pageSize" value="${requestScope.pageable.pageSize}"/>
      </hst:renderURL>
      <c:if test="${index.first and requestScope.pageable.previous}">
        <hst:renderURL var="pageUrlPrevious">
          <hst:param name="page" value="${requestScope.pageable.previousPage}"/>
          <hst:param name="pageSize" value="${requestScope.pageable.pageSize}"/>
        </hst:renderURL>
        <li><a href="${pageUrlPrevious}"><fmt:message key="page.previous" var="prev"/><c:out value="${prev}"/></a></li>
      </c:if>
      <c:choose>
        <c:when test="${requestScope.pageable.currentPage eq pageNr}">
          <li class="active"><a href="#">${pageNr}</a></li>
        </c:when>
        <c:otherwise>
          <li><a href="${pageUrl}">${pageNr}</a></li>
        </c:otherwise>
      </c:choose>
      <c:if test="${index.last and requestScope.pageable.next}">
        <hst:renderURL var="pageUrlNext">
          <hst:param name="page" value="${requestScope.pageable.nextPage}"/>
          <hst:param name="pageSize" value="${requestScope.pageable.pageSize}"/>
        </hst:renderURL>
        <li><a href="${pageUrlNext}"><fmt:message key="page.next" var="next"/><c:out value="${next}"/></a></li>
      </c:if>
    </c:forEach>
  </c:if>
</ul>
