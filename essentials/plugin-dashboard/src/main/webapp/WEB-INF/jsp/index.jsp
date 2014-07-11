<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --%>
<!doctype html>
<html>
<head>
  <title>Hippo Essentials</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.min.css"/>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/components/chosen/chosen.css"/>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/essentials-less/hippo-essentials.css"/>

  <script src="${pageContext.request.contextPath}/components/jquery/jquery.js"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular.js"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular-sanitize.min.js"></script>
  <script src="${pageContext.request.contextPath}/components/chosen/chosen.jquery.js"></script>
  <script src="${pageContext.request.contextPath}/components/underscore/underscore.js"></script>

  <script src="${pageContext.request.contextPath}/components/bootstrap/dist/js/bootstrap.js"></script>


  <%--  NOTE: enable once R&D team upgrades version(s)--%>
  <%--

    <script src="${pageContext.request.contextPath}/components/angular-bootstrap/ui-bootstrap.min.js"></script>
    <script src="${pageContext.request.contextPath}/components/angular-bootstrap/ui-bootstrap-tpls.min.js"></script>
  --%>
  <script src="${pageContext.request.contextPath}/js/lib/ui-bootstrap-tpls.min.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-route.min.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-ui-router.js"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-animate.js"></script>

  <script src="${pageContext.request.contextPath}/js/lib/chosen.js"></script>

  <%-- HIPPO THEME DEPS --%>
  <script src="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.js"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-plugins/dist/js/main.js"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-theme/dist/js/main.js"></script>

  <%-- ESSENTIALS --%>
  <script src="${pageContext.request.contextPath}/js/Essentials.js"></script>
  <script src="${pageContext.request.contextPath}/js/app.js"></script>
  <script src="${pageContext.request.contextPath}/js/routes.js"></script>
  <script src="${pageContext.request.contextPath}/js/directives.js"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js"></script>

  <link rel="icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
</head>
<body id="container" ng-cloak ng-class="feedbackMessages.length ? 'body-push':''">
<essentials-notifier ng-show="feedbackMessages.length" messages="feedbackMessages"></essentials-notifier>
<div class="container-fluid">
  <div class="col-md-2">
    <ul class="side-menu" ng-controller="mainMenuCtrl">
      <li ng-repeat="item in menu" ng-class="{true:'active', false:''}[isPageSelected('{{item.link}}')]">
        <a href="{{item.link}}" ng-click="onMenuClick(item)">
          <i class="fa fa-dashboard"></i>
          <span>{{item.name}}</span></a>
      </li>
      <li>
        <a target="_blank" href="https://issues.onehippo.com/rest/collectors/1.0/template/form/a23eddf8?os_authType=none">
          <i class="fa fa-external-link"></i>
          <span>Feedback</span></a>
      </li>
    </ul>
  </div>
  <div class="col-md-10" ui-view autoscroll="false">
  </div>
</div>

<!-- Include the loader.js script -->
<script src="${pageContext.request.contextPath}/js/loader.js" data-modules="http://localhost:8080/essentials/rest/plugins/modules"></script>

</body>
</html>