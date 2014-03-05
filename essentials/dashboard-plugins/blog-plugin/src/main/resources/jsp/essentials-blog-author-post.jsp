<%@ include file="/WEB-INF/jsp/essentials/common/imports.jsp" %>
<%--@elvariable id="item" type="{{beansPackage}}.Blogpost"--%>
<%--@elvariable id="showPagination" type="java.lang.Boolean"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<h1>${pageable.total}</h1>
<c:if test="${(pageable ne null) and(pageable.total gt 0)}">
  <div class="panel panel-default">
    <div class="panel-heading">
      <h3 class="panel-title">More by this author</h3>
    </div>
    <div class="panel-body">
      <c:forEach var="item" items="${pageable.items}" varStatus="status">
        <hst:link var="link" hippobean="${item}"/>
        <p><a href="${link}"><c:out value="${item.title}"/></a></p>
      </c:forEach>
    </div>
  </div>
</c:if>