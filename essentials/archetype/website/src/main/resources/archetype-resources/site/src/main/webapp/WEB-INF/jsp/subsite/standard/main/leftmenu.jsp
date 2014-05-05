#set( $symbol_dollar = '$' )
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>

<c:if test="${symbol_dollar}{not empty menu.siteMenuItems}">
  <ul class="nav nav-pills nav-stacked">
    <c:forEach var="item" items="${symbol_dollar}{menu.siteMenuItems}">
      <tag:menuitem siteMenuItem="${symbol_dollar}{item}"/>
    </c:forEach>
  </ul>
</c:if>
<hst:cmseditmenu menu="${symbol_dollar}{menu}"/>
