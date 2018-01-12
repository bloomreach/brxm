<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<c:if test="${(requestScope.pageable ne null) && (not empty requestScope.pageable.items)}">
  <div>
    <c:forEach var="item" items="${requestScope.pageable.items}">
      <c:choose>
        <c:when test="${hst:isReadable(item, 'title')}">
          <c:set var="linkName" value="${item.title}"/>
        </c:when>
        <c:otherwise>
          <c:set var="linkName" value="${item.localizedName}"/>
        </c:otherwise>
      </c:choose>

      <article class="has-edit-button">
        <hst:manageContent document="${item}"/>
        <hst:link var="link" hippobean="${item}"/>
        <h3><a href="${link}"><c:out value="${linkName}"/></a></h3>
        <c:if test="${hst:isReadable(item, 'introduction')}">
          <p><c:out value="${item.introduction}"/></p>
        </c:if>
      </article>
    </c:forEach>
    <c:if test="${requestScope.cparam.showPagination}">
      <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
    </c:if>
  </div>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode and empty requestScope.pageable}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/generic-list.png'/>"> Click to edit Generic List
  </div>
</c:if>