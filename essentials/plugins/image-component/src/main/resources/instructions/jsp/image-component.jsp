<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean"--%>
<hst:link var="img" hippobean="${document.original}"/>
<img src="${img}" title="${fn:escapeXml(document.fileName)}" alt="${fn:escapeXml(document.fileName)}"/>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${editMode && (document eq null)}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/image.png'/>"> Click to edit Image
</c:if>
