<!doctype html>
<html data-ng-app="Essentials">
<head>
  <title>Hippo Essentials</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/theme/hippo-theme/main.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/hippo-essentials.css"/>
  <%--<script src="${pageContext.request.contextPath}/js/jquery.js"></script>--%>
  <script src="${pageContext.request.contextPath}/js/angular.js"></script>
  <script src="${pageContext.request.contextPath}/js/angular-route.min.js"></script>


  <%--<script src="${pageContext.request.contextPath}/js/require.js" data-main="${pageContext.request.contextPath}/js/main.js"></script>--%>
  <%--<script src="${pageContext.request.contextPath}/js/require.js"></script>--%>

  <!--<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.3/angular.min.js"></script>-->
<%--  <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.3/angular-route.js"></script>
  <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>--%>
  <script src="${pageContext.request.contextPath}/js/Essentials.js"></script>
  <script src="${pageContext.request.contextPath}/js/app.js"></script>
  <script src="${pageContext.request.contextPath}/js/routes.js"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js"></script>
  <%--  TODO make dynamic--%>
  <script src="${pageContext.request.contextPath}/plugins/contentBlocks/controller.js"></script>
  <script src="${pageContext.request.contextPath}/plugins/newsEventsPowerpack/controller.js"></script>

</head>
<body>
<!-- LOADER ON HTTP REQUESTS -->
<div class="busy-loader" ng-show="busyLoading">
  <img src="${pageContext.request.contextPath}/images/loader.gif"/>
</div>
<!-- ERROR MESSAGES -->
<div class="alert-danger" ng-show="globalError.length > 0">
  <strong>An error occurred:</strong>
  <div ng-repeat="message in globalError">
    {{message}}
  </div>
</div>
<%--
  CONTENT
--%>

<div class="container-full">
  <div class="hippo-header">
    <div class="hippo-header-logo">
      <img src="${pageContext.request.contextPath}/images/hippo-logo.png">
    </div>
    <div class="hippo-header-text">
           {{headerMessage}}
    </div>

  </div>
  <div class="col-sm-2" style="margin-right: 20px;" ng-controller="mainMenuCtrl">
    <ul class="nav nav-stacked nav-pills" ng-show="packsInstalled">
      <li ng-repeat="item in menu" ng-class="{true:'active', false:''}[isPageSelected('{{item.link}}')]">
        <a  href="{{item.link}}" ng-click="onMenuClick(item)">{{item.name}}</a>
      </li>
    </ul>

  </div>
  <div class="col-sm-9" ng-controller="onLoadCtrl">
    <div ng-view></div>
  </div>
</div>

<div class="footer">&copy; Hippo B.V., All Rights Reserved</div>


</body>
</html>