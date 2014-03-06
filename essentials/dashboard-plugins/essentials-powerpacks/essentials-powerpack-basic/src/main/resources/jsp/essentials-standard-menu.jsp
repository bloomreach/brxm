<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
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
