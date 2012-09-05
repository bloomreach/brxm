<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>

<ul class="sitenav">
  <c:forEach var="item" items="${menu.siteMenuItems}">
    <li>
      <tag:menuitem siteMenuItem="${item}"/>
    </li>
  </c:forEach>
</ul>
