<%@ include file="/WEB-INF/jsp/essentials/common/imports.jsp" %>
<%--@elvariable id="banner" type="{{beansPackage}}.Banner"--%>
<div class="row">
  <a href="<hst:link hippobean="${banner.link}" />"><img src="<hst:link hippobean="${banner.image}" />" alt="${banner.title}"/></a>
</div>
