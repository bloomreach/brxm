<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Banner"--%>
<div>
  <a href="<hst:link hippobean="${requestScope.document.link}" />"><img src="<hst:link hippobean="${requestScope.document.image}" />" alt="${fn:escapeXml(requestScope.document.title)}"/></a>
</div>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode && (requestScope.document eq null)}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
</c:if>
