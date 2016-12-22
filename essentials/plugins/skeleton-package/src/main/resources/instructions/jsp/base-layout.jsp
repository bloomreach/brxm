<!doctype html>
<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<html lang="en">
  <head>
    <meta charset="utf-8"/>
    <link rel="stylesheet" href="<hst:webfile  path="/css/bootstrap.css"/>" type="text/css"/>
    <c:if test="${hstRequest.requestContext.cmsRequest}">
      <link rel="stylesheet" href="<hst:webfile  path="/css/cms-request.css"/>" type="text/css"/>
    </c:if>
    <hst:headContributions categoryExcludes="htmlBodyEnd, scripts" xhtml="true"/>
  </head>
  <body>
    <div class="container">
      <div class="row">
        <div class="col-md-6 col-md-offset-3">
          <hst:include ref="top"/>
        </div>
      </div>
      <div class="row">
        <div class="col-md-6 col-md-offset-3">
          <hst:include ref="menu"/>
        </div>
      </div>
      <div class="row">
        <hst:include ref="main"/>
      </div>
      <div class="row">
        <hst:include ref="footer"/>
      </div>
    </div>
    <hst:headContributions categoryIncludes="htmlBodyEnd, scripts" xhtml="true"/>
  </body>
</html>