<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="item" type="{{beansPackage}}.Blogpost"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<c:if test="${requestScope.pageable ne null}">
  <div>
    <c:forEach var="item" items="${requestScope.pageable.items}" varStatus="status">
      <hst:link var="link" hippobean="${item}"/>
      <article class="has-edit-button">
        <%-- No possibility to create new blog documents since not all fields types are supported --%>
        <hst:managecontent document="${item}"/>
        <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
        <c:if test="${hst:isReadable(item, 'publicationDate.time')}">
          <p>
            <fmt:formatDate value="${item.publicationDate.time}" type="both" dateStyle="medium" timeStyle="short"/>
          </p>
        </c:if>
        <p><c:out value="${item.introduction}"/></p>
      </article>
    </c:forEach>
    <c:if test="${requestScope.cparam.showPagination}">
      <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
    </c:if>
  </div>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode and requestScope.pageable eq null}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/blog-list.png'/>"> Click to edit Blog List
  </div>
</c:if>
