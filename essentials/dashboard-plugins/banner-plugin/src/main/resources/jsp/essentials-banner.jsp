<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="document" type="{{beansPackage}}.Banner"--%>
<div class="row">
  <a href="<hst:link hippobean="${document.link}" />"><img src="<hst:link hippobean="${document.image}" />" alt="${document.title}"/></a>
</div>
