<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<hst:setBundle basename="essentials.searchbox"/>
<form class="navbar-form" role="search" action="<hst:link siteMapItemRefId='search'/>" method="get">
  <div class="input-group">
    <fmt:message key='searchbox.placeholder' var="placeholder"/>
    <input type="text" class="form-control" placeholder="${fn:escapeXml(placeholder)}" name="query">
    <div class="input-group-btn">
      <button class="btn btn-default" type="submit"><i class="glyphicon glyphicon-search"></i></button>
    </div>
  </div>
</form>

