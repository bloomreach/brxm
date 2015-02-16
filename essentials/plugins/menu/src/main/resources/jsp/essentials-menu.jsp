<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>
<c:if test="${not empty menu}">
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
  <hst:cmseditmenu menu="${menu}"/>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${editMode && empty menu}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/menu.png'/>"> Click to edit Menu
</c:if>
