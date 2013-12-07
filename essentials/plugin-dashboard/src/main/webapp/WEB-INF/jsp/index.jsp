<!doctype html>
<html data-ng-app="Essentials">
<head>
  <title>Hippo Essentials</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/theme/hippo-theme/main.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/hippo-essentials.css"/>

  <script src="${pageContext.request.contextPath}/js/angular.js"></script>
  <!--<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.3/angular.min.js"></script>-->
  <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.3/angular-route.js"></script>
  <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/js/app.js"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js"></script>

</head>
<body>
<!-- LOADER ON HTTP REQUESTS -->
<div class="busy-loader" ng-show="busyLoading">
  <img src="${pageContext.request.contextPath}/images/loader.gif"/>
</div>
<!-- ERROR MESSAGES -->
<div class="alert-danger" ng-show="globalError">
  <div ng-repeat="message in errorMessage">
    {{ message.message }}
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

    </div>

  </div>
  <div class="col-sm-2" style="margin-right: 20px;" ng-controller="mainMenuCtrl">

    <ul class="nav nav-stacked">
      <li ng-repeat="item in menu">
        <a href="" ng-click="onMenuClick(item)">{{item.item}}</a>
      </li>
    </ul>

  </div>
  <div class="col-sm-9">
    [BODY]
  </div>
</div>

<div class="footer">&copy; Hippo B.V., All Rights Reserved</div>


</body>
</html>