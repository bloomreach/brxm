<#include "/WEB-INF/freemarker/include/imports.ftl">
<%--@elvariable id="document" type="{{beansPackage}}.NewsDocument"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<%--@elvariable id="showPagination" type="java.lang.Boolean"--%>

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
    <p><c:out value="${item.introduction}"/></p>
  </article>
</c:forEach>
<c:if test="${showPagination}">
<#include "/WEB-INF/freemarker/include/pagination.ftl">
</c:if>