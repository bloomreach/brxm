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
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main.css?v=${project.version}"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.min.css?v=${project.version}"/>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/components/chosen/chosen.css?v=${project.version}"/>
  <script src="${pageContext.request.contextPath}/components/jquery/jquery.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular-sanitize.min.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/chosen/chosen.jquery.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/underscore/underscore.js?v=${project.version}"></script>

  <script src="${pageContext.request.contextPath}/components/bootstrap/dist/js/bootstrap.js?v=${project.version}"></script>


  <%--  NOTE: enable once R&D team upgrades version(s)--%>
  <%--

    <script src="${pageContext.request.contextPath}/components/angular-bootstrap/ui-bootstrap.min.js"></script>
    <script src="${pageContext.request.contextPath}/components/angular-bootstrap/ui-bootstrap-tpls.min.js"></script>
  --%>
  <script src="${pageContext.request.contextPath}/js/lib/ui-bootstrap-tpls.min.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-route.min.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-ui-router.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-animate.js?v=${project.version}"></script>

  <script src="${pageContext.request.contextPath}/js/lib/chosen.js?v=${project.version}"></script>

  <%-- HIPPO THEME DEPS --%>
  <script src="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-plugins/dist/js/main.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-theme/dist/js/main.js?v=${project.version}"></script>

  <%-- ESSENTIALS --%>
  <script src="${pageContext.request.contextPath}/js/Essentials.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/app.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/routes.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/directives.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js?v=${project.version}"></script>

  <link rel="icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
</head>
<body id="container" ng-cloak ng-class="feedbackMessages.length ? 'body-push':''">
<essentials-notifier ng-show="feedbackMessages.length" messages="feedbackMessages"></essentials-notifier>

<div class="hippo-navbar navbar navbar-default">
  <div class="container-fluid">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".sidebar-navbar-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <span class="badge notification-badge">18</span>
      <a class="navbar-brand" href="#">Hippo Essentials</a>
      <%--<p class="navbar-text pull-left">
        version: ${project.version}
      </p>--%>
      <div ng-show="NEEDS_REBUILD" class="navbar-text navbar-icons">
        <a href="#/build" class="navbar-link">
          <span class="fa fa-refresh"></span> System needs a rebuild
        </a>
        <a href="#/build" class="navbar-link">
         <span  class="fa-stack">
                            <span class="fa fa-circle fa-stack-2x fa-white"></span>
                            <span class="fa fa-bell-o fa-stack-1x fa-danger"></span>
         </span>

        </a>
      </div>
    </div>
  </div>
</div>

<div class="navbar-collapse sidebar-navbar-collapse collapse ng-scope" ng-controller="mainMenuCtrl">
  <ul class="nav navbar-nav">
    <li ng-show="INSTALLED_FEATURES > 0" ng-class="{true:'active', false:''}[isPageSelected('#/installed-features')]">
      <a href="#/installed-features">
        <i class="fa fa-gears fa-2x fa-fw fa-middle"></i>
        <span>Installed features</span>
        <span ng-show="TOTAL_NEEDS_ATTENTION > 0" class="badge pull-right alert-success">{{TOTAL_NEEDS_ATTENTION}}</span>
      </a>
    </li>
    <li ng-class="{true:'active', false:''}[isPageSelected('#/library')]">
      <a href="#/library">
        <i class="fa fa-gear fa-2x fa-fw fa-middle"></i>
        <span>Library</span>
        <!--            <span class="badge pull-right alert-info">{{TOTAL_PLUGINS}}</span> -->
      </a>

    </li>
    <li ng-class="{true:'active', false:''}[isPageSelected('#/tools')]">
      <a href="#/tools">
        <i class="fa fa-gavel fa-2x fa-fw fa-middle"></i>
        <span>Tools</span>
        <!--            <span class="badge  pull-right alert-info">{{TOTAL_TOOLS}}</span> -->
      </a>
    </li>
    <li ng-class="{true:'active', false:''}[isPageSelected('#/build')]">
      <a href="#/build">
        <i ng-hide="NEEDS_REBUILD" class="fa fa-check-square-o fa-2x fa-fw fa-middle"></i>
        <i ng-show="NEEDS_REBUILD" class="fa fa-exclamation-triangle fa-2x fa-fw fa-middle"></i>
        <span>Build</span>
            <span ng-show="NEEDS_REBUILD" class="badge pull-right alert-danger">
              <i class="fa fa-bell"></i>
            </span>
            <span ng-hide="NEEDS_REBUILD" class="badge pull-right alert-success">
              <i class="fa fa-check"></i>
            </span>
      </a>
    </li>
    <li>
      <a target="_blank" href="https://issues.onehippo.com/rest/collectors/1.0/template/form/a23eddf8?os_authType=none">
        <i class="fa fa-external-link fa-2x fa-fw fa-middle"></i>
        <span>Feedback</span></a>
    </li>
  </ul>
</div>

<div class="container-fluid components-container">
  <div class="row">
    <div class="col-lg-12" ui-view autoscroll="false">
    </div>
  </div>
  <div class="row">
    <div class="col-lg-12">
      <p class="text-center" id="footer">
        <em>version: ${project.version}</em>
      </p>
    </div>
  </div>
</div>
<!-- Include the loader.js script -->
<script src="${pageContext.request.contextPath}/js/loader.js" data-modules="http://localhost:8080/essentials/rest/plugins/modules"></script>

</body>
</html>