<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="item" type="{{beansPackage}}.Blogpost"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<hst:setBundle basename="essentials.blog"/>
<c:if test="${requestScope.pageable ne null}">
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
        <fmt:message key="blog.read.post" var="msg"/>
        <p><a href="${link}"><c:out value="${msg}"/></a></p>
      </article>
    </c:forEach>
    <div class="has-new-content-button">
      <hst:manageContent documentTemplateQuery="new-blog-document" rootPath="blog" defaultPath="${currentYear}/${currentMonth}"/>
    </div>
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
