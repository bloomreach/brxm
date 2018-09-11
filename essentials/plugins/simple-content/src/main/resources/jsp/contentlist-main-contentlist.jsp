<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="item" type="{{beansPackage}}.ContentDocument"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>

<div>
  <c:forEach var="item" items="${requestScope.pageable.items}" varStatus="status">
    <hst:link var="link" hippobean="${item}"/>
    <article class="has-edit-button">
      <hst:manageContent hippobean="${item}"/>
      <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
      <c:if test="${hst:isReadable(item, 'publicationDate.time')}">
        <p>
          <fmt:formatDate value="${item.publicationDate.time}" type="both" dateStyle="medium" timeStyle="short"/>
        </p>
      </c:if>
      <p><c:out value="${item.introduction}"/></p>
    </article>
  </c:forEach>
  <div class="has-new-content-button">
    <hst:manageContent documentTemplateQuery="new-content-document" rootPath="content"/>
  </div>
  <c:if test="${requestScope.cparam.showPagination}">
    <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
  </c:if>
</div>