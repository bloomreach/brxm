<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Banner"--%>
<div class="row">
  <a href="<hst:link hippobean="${document.link}" />"><img src="<hst:link hippobean="${document.image}" />" alt="${fn:escapeXml(document.title)}"/></a>
</div>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${editMode && (document eq null)}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
</c:if>
