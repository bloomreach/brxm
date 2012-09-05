<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="tag" tagdir="/WEB-INF/tags" %>

<%@ attribute name="siteMenuItem" type="org.hippoecm.hst.core.sitemenu.HstSiteMenuItem" rtexprvalue="true" required="true"%>

<c:choose>
  <c:when test="${siteMenuItem.selected}">
    <b><c:out value="${siteMenuItem.name}"/></b>
  </c:when>
  <c:otherwise>
    <hst:link var="link" link="${siteMenuItem.hstLink}"/>
    <c:if test="${empty link}">
      <c:set var="link" value="${siteMenuItem.externalLink}"/>
    </c:if>
    <a href="${link}"><c:out value="${siteMenuItem.name}"/></a>
  </c:otherwise>
</c:choose>
<c:if test="${siteMenuItem.expanded and not empty siteMenuItem.childMenuItems}">
  <ul>
    <c:forEach var="child" items="${siteMenuItem.childMenuItems}">
      <li>
        <tag:menuitem siteMenuItem="${child}"/>
      </li>
    </c:forEach>
  </ul>
</c:if>
