<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<c:forEach var="item" items="${pageable.items}">
  <c:if test="${hst:isReadable(item, 'title')}">
    <hst:link var="link" hippobean="${item}"/>
    <article class="has-edit-button">
      <hst:cmseditlink hippobean="${item}"/>
      <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
      <c:if test="${hst:isReadable(item, 'introduction')}">
        <p><c:out value="${item.introduction}"/></p>
      </c:if>
    </article>
  </c:if>
</c:forEach>
<c:if test="${pageable.showPagination}">
  <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${editMode and empty pageable}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/generic-list.png'/>"> Click to edit Generic List
</c:if>