<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<hst:defineObjects/>
<c:if test="${hstRequest.requestContext.preview}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/seo.png'/>"> Click to edit SEO
</c:if>
