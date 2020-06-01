<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:choose>
  <c:when test="${requestScope.menu ne null}">
    <div class="has-edit-button">
      <c:if test="${not empty requestScope.menu.siteMenuItems}">
        <ul class="nav nav-pills">
          <c:forEach var="item" items="${requestScope.menu.siteMenuItems}">
            <c:choose>
              <c:when test="${empty item.hstLink && empty item.externalLink}">
                <c:choose>
                  <c:when test="${item.selected or item.expanded}">
                    <li class="active"><div style="padding: 10px 15px;"><c:out value="${item.name}"/></div></li>
                  </c:when>
                  <c:otherwise>
                    <li><div style="padding: 10px 15px;"><c:out value="${item.name}"/></div></li>
                  </c:otherwise>
                </c:choose>
              </c:when>
              <c:otherwise>
                <c:choose>
                  <c:when test="${not empty item.hstLink}"><c:set var="href"><hst:link link="${item.hstLink}"/></c:set></c:when>
                  <c:when test="${not empty item.externalLink}"><c:set var="href">${fn:escapeXml(item.externalLink)}</c:set></c:when>
                </c:choose>
                <c:choose>
                  <c:when test="${item.selected or item.expanded}">
                    <li class="active"><a href="${href}"><c:out value="${item.name}"/></a></li>
                  </c:when>
                  <c:otherwise>
                    <li><a href="${href}"><c:out value="${item.name}"/></a></li>
                  </c:otherwise>
                </c:choose>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </ul>
      </c:if>
      <hst:cmseditmenu menu="${requestScope.menu}"/>
    </div>
  </c:when>
</c:choose>
