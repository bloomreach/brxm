<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="item" type="{{beansPackage}}.Blogpost"--%>
<%--@elvariable id="author" type="{{beansPackage}}.Author"--%>
<%--@elvariable id="showPagination" type="java.lang.Boolean"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<div class="panel panel-default">
  <c:if test="${(pageable ne null)}">
    <div class="panel-heading">
      <h3 class="panel-title">More by ${author.fullName}</h3>
    </div>
    <c:choose>
      <c:when test="${pageable.total gt 0}">
        <div class="panel-body">
          <c:forEach var="item" items="${pageable.items}" varStatus="status">
            <hst:link var="link" hippobean="${item}"/>
            <p><a href="${link}"><c:out value="${item.title}"/></a></p>
          </c:forEach>
        </div>
      </c:when>
      <c:otherwise>
        <div class="panel-body">
          <p>No other posts found.</p>
        </div>
      </c:otherwise>
    </c:choose>
  </c:if>
</div>