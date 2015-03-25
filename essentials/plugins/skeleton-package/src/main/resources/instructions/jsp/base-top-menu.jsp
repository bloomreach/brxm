<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:choose>
  <c:when test="${menu ne null}">
    <c:if test="${not empty menu.siteMenuItems}">
      <ul class="nav nav-pills">
        <c:forEach var="item" items="${menu.siteMenuItems}">
          <c:choose>
            <c:when test="${item.selected or item.expanded}">
              <li class="active"><a href="<hst:link link="${item.hstLink}"/>"><c:out value="${item.name}"/></a></li>
            </c:when>
            <c:otherwise>
              <li><a href="<hst:link link="${item.hstLink}"/>"><c:out value="${item.name}"/></a></li>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </ul>
    </c:if>
    <hst:cmseditmenu menu="${menu}"/>
  </c:when>

  <%--Placeholder reminding us to configure a valid menu in the component parameters--%>
  <c:otherwise>
    <c:if test="${editMode}">
      <h5>[Menu Component]</h5>
      <sub>Click to edit Menu</sub>
    </c:if>
  </c:otherwise>
</c:choose>
