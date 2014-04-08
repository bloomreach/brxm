<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<%--@elvariable id="pageSize" type="java.lang.Integer"--%>
<%--@elvariable id="page" type="java.lang.Integer"--%>
<ul class="pagination">
  <li class="disabled"><a href="#">${pageable.total} document(s)</a></li>
  <c:forEach var="pageNr" items="${pageable.pageNumbersArray}" varStatus="index">
    <hst:renderURL var="pageUrl">
      <hst:param name="page" value="${pageNr}"/>
      <hst:param name="pageSize" value="${pageSize}"/>
    </hst:renderURL>
    <c:if test="${index.first and pageable.previous}">
      <hst:renderURL var="pageUrlPrevious">
        <hst:param name="page" value="${pageNr}"/>
        <hst:param name="pageSize" value="${pageSize}"/>
      </hst:renderURL>
      <li><a href="${pageUrlPrevious}">previous</a></li>
    </c:if>
    <c:choose>
      <c:when test="${page eq pageNr}">
        <li class="active"><a href="#">${pageNr}</a></li>
      </c:when>
      <c:otherwise>
        <li><a href="${pageUrl}">${pageNr}</a></li>
      </c:otherwise>
    </c:choose>
    <c:if test="${index.last and pageable.next}">
      <hst:renderURL var="pageUrlNext">
        <hst:param name="page" value="${pageNr}"/>
        <hst:param name="pageSize" value="${pageSize}"/>
      </hst:renderURL>
      <li><a href="${pageUrlNext}">next</a></li>
    </c:if>
  </c:forEach>
</ul>
