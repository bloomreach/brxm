<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Banner"--%>
<c:if test="${requestScope.document ne null}">
  <div>
    <a href="<hst:link hippobean="${requestScope.document.link}" />">
      <figure style="position: relative">
        <hst:manageContent document="${document}" componentParameter="document" rootPath="banners"/>
        <img src="<hst:link hippobean="${requestScope.document.image}"/>" alt="${fn:escapeXml(requestScope.document.title)}"/>
        <figcaption style="position:absolute; top:20px; left:20px; z-index:100; color:white; background: rgba(51, 122, 183, 0.7); width:60%; padding:0 20px 20px 20px; text-shadow: 0 1px 2px rgba(0, 0, 0, .6);">
          <c:if test="${not empty requestScope.document.title}">
            <h3>${requestScope.document.title}</h3>
          </c:if>
          <hst:html hippohtml="${requestScope.document.content}"/>
        </figcaption>
      </figure>
    </a>
  </div>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode && (requestScope.document eq null)}">
  <div>
    <figure style="position: relative">
      <hst:manageContent templateQuery="new-banner-document" componentParameter="document" rootPath="banners"/>
      <img src="<hst:link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
    </figure>
  </div>
</c:if>