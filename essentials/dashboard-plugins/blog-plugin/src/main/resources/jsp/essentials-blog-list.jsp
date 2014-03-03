<%@ include file="/WEB-INF/jsp/essentials/common/imports.jsp" %>
<%--@elvariable id="item" type="{{beansPackage}}.Blogpost"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>

<c:forEach var="item" items="${pageable.items}" varStatus="status">
  <hst:link var="link" hippobean="${item}"/>
  <article>
    <hst:cmseditlink hippobean="${item}"/>
    <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
    <c:if test="${hst:isReadable(item, 'publicationDate.time')}">
      <p>
        <fmt:formatDate value="${item.publicationDate.time}" type="both" dateStyle="medium" timeStyle="short"/>
      </p>
    </c:if>
    <p><c:out value="${item.introduction}"/></p>
  </article>
</c:forEach>
<c:if test="${showPagination}">
  <%@ include file="/WEB-INF/jsp/essentials/common/pagination.jsp" %>
</c:if>