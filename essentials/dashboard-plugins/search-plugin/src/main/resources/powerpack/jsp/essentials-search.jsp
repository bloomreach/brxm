<%@ include file="/WEB-INF/jsp/essentials/common/imports.jsp" %>
<form class="navbar-form" role="search" action="<hst:link path="/search" />" method="get">
  <div class="input-group">
    <input type="text" class="form-control" placeholder="Search" name="query">
    <div class="input-group-btn">
      <button class="btn btn-default" type="submit"><i class="glyphicon glyphicon-search"></i></button>
    </div>
  </div>
</form>