<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>
<ul class="nav nav-pills">
  <c:forEach var="item" items="${menu.siteMenuItems}">
    <c:choose>
      <c:when test="${item.selected or item.expanded}">
        <li class="active"><a href="<hst:link link="${item.hstLink}"/>">${item.name}</a></li>
      </c:when>
      <c:otherwise>
        <li><a href="<hst:link link="${item.hstLink}"/>">${item.name}</a></li>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</ul>
