<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Video"--%>
<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsVideoComponentInfo"--%>
<c:if test="${not empty document}">
  <h3><c:out value="${document.title}"/></h3>
  <iframe width="${cparam.width}" height="${cparam.height}" src="${fn:escapeXml(document.link)}" frameborder="0" allowfullscreen></iframe>
  <p><c:out value="${document.description}"/></p>
</c:if>
<c:if test="${editMode && empty document}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/video.png'/>"> Click to edit Video
</c:if>

