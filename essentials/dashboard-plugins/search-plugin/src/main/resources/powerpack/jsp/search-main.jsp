<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="item" type="org.example.beans.NewsDocument"--%>
<%--@elvariable id="query" type="java.lang.String"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<%--@elvariable id="showPagination" type="java.lang.Boolean"--%>
<c:if test="${pageable.total == 0}">
  <h3>No results for: <c:out value="${query}"/></h3>
</c:if>
<c:forEach var="item" items="${pageable.items}" varStatus="status">
  <hst:link var="link" hippobean="${item}"/>
  <article>
    <hst:cmseditlink hippobean="${item}"/>
    <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
  </article>
</c:forEach>
<c:if test="${showPagination}">
  <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
</c:if>
