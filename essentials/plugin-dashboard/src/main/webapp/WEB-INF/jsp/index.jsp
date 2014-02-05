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
  <script src="${pageContext.request.contextPath}/plugins/galleryPlugin/controller.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/newsEventsPowerpack/controller.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/xinhaPlugin/xinhaPlugin.js"></script>
  <script src="${pageContext.request.contextPath}/tools/beanwriter/beanwriter.js"></script>
  <link rel="icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>

</head>
<body>
<!-- LOADER ON HTTP REQUESTS -->
<div class="busy-loader" ng-show="busyLoading">
  <img src="${pageContext.request.contextPath}/images/loader.gif"/>
</div>
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

<div class="container-full">
  <div class="hippo-header">
    <div class="hippo-header-logo">
      <a href="${pageContext.request.contextPath}"><img src="${pageContext.request.contextPath}/images/hippo-logo.png"></a>
    </div>
    <div class="hippo-header-text">
      {{headerMessage}}
    </div>

  </div>
  <div class="col-sm-2" style="margin-right: 20px;" ng-controller="mainMenuCtrl">
    <ul class="nav nav-stacked nav-pills" ng-show="packsInstalled">
      <li ng-repeat="item in menu" ng-class="{true:'active', false:''}[isPageSelected('{{item.link}}')]">
        <a href="{{item.link}}" ng-click="onMenuClick(item)">{{item.name}}</a>
      </li>
    </ul>

  </div>
  <div class="col-sm-9" ng-controller="homeCtrl">
    <div ui-view="submenu" autoscroll="false"></div>
    <div ui-view="plugintabs" autoscroll="false"></div>
    <div style="margin-left: 220px;" ui-view="plugininstance" autoscroll="false"></div>
    <div ui-view autoscroll="false"></div>
    <%--<div ng-view></div>--%>
  </div>
  <div class="clearfix"></div>
  <div class="col-sm-12 footer">&copy; Hippo B.V., All Rights Reserved</div>
</div>


</body>
</html>