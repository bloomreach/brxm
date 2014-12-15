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
