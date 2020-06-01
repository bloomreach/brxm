<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Video"--%>
<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsVideoComponentInfo"--%>
<c:if test="${not empty requestScope.document}">
  <div>
    <h3><c:out value="${requestScope.document.title}"/></h3>
    <iframe width="${requestScope.cparam.width}" height="${requestScope.cparam.height}" src="${fn:escapeXml(requestScope.document.link)}" frameborder="0" allowfullscreen></iframe>
    <p><c:out value="${requestScope.document.description}"/></p>
</div>
</c:if>
<c:if test="${requestScope.editMode && empty requestScope.document}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/video.png'/>"> Click to edit Video
  </div>
</c:if>

