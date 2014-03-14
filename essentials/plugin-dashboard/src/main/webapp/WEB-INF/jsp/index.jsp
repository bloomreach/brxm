<!doctype html>
<html ng-app="hippo.essentials">
<head>
  <title>Hippo Essentials</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/theme/hippo-theme/main.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/hippo-essentials.css"/>
  <%--<script src="${pageContext.request.contextPath}/js/jquery.js"></script>--%>
  <script src="http://ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/jquery.ui.min.js"></script>

  <script src="${pageContext.request.contextPath}/js/lib/angular.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-route.min.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-ui-router.js"></script>

  <script src="${pageContext.request.contextPath}/js/lib/chosen.jquery.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/chosen.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/ui-sortable.js"></script>

  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/chosen.css"/>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/chosen-spinner.css"/>
  <%--<script src="${pageContext.request.contextPath}/js/require.js" data-main="${pageContext.request.contextPath}/js/main.js"></script>--%>
  <%--<script src="${pageContext.request.contextPath}/js/require.js"></script>--%>

  <!--<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.3/angular.min.js"></script>-->
  <%--  <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.3/angular-route.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>--%>
  <script src="${pageContext.request.contextPath}/js/lib/ui-bootstrap-0.10.js"></script>

  <script src="${pageContext.request.contextPath}/js/Essentials.js"></script>
  <script src="${pageContext.request.contextPath}/js/app.js"></script>
  <script src="${pageContext.request.contextPath}/js/routes.js"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js"></script>
  <%--  TODO make dynamic--%>
  <script src="${pageContext.request.contextPath}/plugins/contentBlocks/contentBlocks.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/galleryPlugin/galleryPlugin.js"></script>
  <script src="${pageContext.request.contextPath}/powerpacks/newsEventsPowerpack/newsEventsPowerpack.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/xinhaPlugin/xinhaPlugin.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/blogPlugin/blogPlugin.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/carouselPlugin/carouselPlugin.js"></script>
  <script src="${pageContext.request.contextPath}/tools/beanwriter/beanwriter.js"></script>
  <script src="${pageContext.request.contextPath}/tools/freemarkersync/freemarkersync.js"></script>
  <script src="${pageContext.request.contextPath}/tools/restServices/restServices.js"></script>
  <link rel="icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
</head>
<body>
<!-- ERROR MESSAGES -->
<div class="alert-danger messages" ng-show="globalError.length > 0">
  <strong>An error occurred:</strong>
  <div ng-repeat="message in globalError">
    {{message}}
  </div>
</div>
<div class="alert-success messages" ng-show="feedbackMessages.length > 0">
  <div ng-repeat="message in feedbackMessages">
    <strong>{{message}}</strong>
  </div>
</div>
<%--
  CONTENT
--%>

<div class="container">
  <div class="row">
    <h1 class="page-header">
      <%--<div class="pull-left"><img src="${pageContext.request.contextPath}/images/hippo-logo-2x.png" height="30"/></div>--%>
        <!-- LOADER ON HTTP REQUESTS -->
        <div class="pull-right" ng-show="busyLoading">
          <span class="fa fa-spin fa-refresh"></span>&nbsp;
        </div>
        Hippo CMS <small>Essentials</small></h1>
    </div>

  <div class="row">
    <div class="col-sm-2" style="margin-right: 20px;" ng-controller="mainMenuCtrl">
      <ul class="nav nav-stacked nav-pills" ng-show="packsInstalled">
        <li ng-repeat="item in menu" ng-class="{true:'active', false:''}[isPageSelected('{{item.link}}')]">
          <a href="{{item.link}}" ng-click="onMenuClick(item)">{{item.name}}</a>
        </li>
        <li>
          <a target="API" href="${pageContext.request.contextPath}/docs/rest-api/index.html">REST API</a>

        </li>
      </ul>
    </div>
  </div>

  <div ng-controller="homeCtrl">
    <div ui-view="submenu" autoscroll="false"></div>
    <div ui-view="plugintabs" autoscroll="false"></div>
    <div ui-view="plugininstance" autoscroll="false"></div>

    <%--Main view--%>
    <div ui-view autoscroll="false"></div>
    <%--/ Main view--%>

  </div>

  <div class="row">
    <p class="text-center">
      (C) 2013-2014 <a href="http://www.onehippo.com">Hippo B.V.</a>, All Rights Reserved
    </p>
  </div>
</div>


</body>
</html>