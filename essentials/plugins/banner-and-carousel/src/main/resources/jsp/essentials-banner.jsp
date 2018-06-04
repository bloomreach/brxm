<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.Banner"--%>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.document ne null}">
  <div>
    <c:choose>
      <c:when test="${requestScope.document['class'].name eq '{{beansPackage}}.Banner'}">
        <a href="<hst:link hippobean="${requestScope.document.link}" />">
          <figure style="position: relative">
            <hst:manageContent hippobean="${document}" parameterName="document" rootPath="banners"/>
            <img src="<hst:link hippobean="${requestScope.document.image}"/>" alt="${fn:escapeXml(requestScope.document.title)}"/>
            <figcaption style="position:absolute; top:20px; left:20px; z-index:100; color:white; background: rgba(51, 122, 183, 0.7); width:60%; padding:0 20px 20px 20px; text-shadow: 0 1px 2px rgba(0, 0, 0, .6);">
              <c:if test="${not empty requestScope.document.title}">
                <h3>${requestScope.document.title}</h3>
              </c:if>
              <hst:html hippohtml="${requestScope.document.content}"/>
            </figcaption>
          </figure>
        </a>
      </c:when>
      <c:when test="${requestScope.editMode}">
        <figure style="position: relative">
          <hst:manageContent templateQuery="new-banner-document" parameterName="document" rootPath="banners"/>
          <img src="<hst:link path='/images/essentials/catalog-component-icons/banner.png'/>">  Selected document "${document.node.path}" is not of the correct type, please select or create a Banner document.
        </figure>
      </c:when>
    </c:choose>
  </div>
</c:if>
<c:if test="${requestScope.editMode && (requestScope.document eq null)}">
  <div>
    <figure style="position: relative">
      <hst:manageContent templateQuery="new-banner-document" parameterName="document" rootPath="banners"/>
      <img src="<hst:link path='/images/essentials/catalog-component-icons/banner.png'/>"> Select or create a Banner document
    </figure>
  </div>
</c:if>