<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="document" type="{{beansPackage}}.EventsDocument"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>

<c:forEach var="item" items="${pageable.items}" varStatus="status">
  <hst:link var="link" hippobean="${item}"/>
  <article>
    <hst:cmseditlink hippobean="${item}"/>
    <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
    <c:if test="${hst:isReadable(item, 'date.time')}">
      <p>
        <fmt:formatDate value="${item.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
      </p>
    </c:if>
    <c:if test="${hst:isReadable(item, 'enddate.time')}">
      <p>
        <fmt:formatDate value="${item.enddate.time}" type="both" dateStyle="medium" timeStyle="short"/>
      </p>
    </c:if>
    <p><c:out value="${item.location}"/></p>
  </article>
</c:forEach>
<c:if test="${pageable.showPagination}">
  <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
</c:if>