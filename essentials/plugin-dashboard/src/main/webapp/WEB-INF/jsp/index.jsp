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
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/essentials-less/hippo-essentials.css?v=${project.version}"/>


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
<div class="container-fluid">
  <div class="content fixed-fixed">
    <div class="row-fluid">
      <div class="col-lg-12">
        <div class="col-lg-4"></div>
        <div class="col-lg-4 essentials-header"><h1>{{mainHeader}}</h1></div>
        <div class="col-lg-4">
          <div ng-show="NEEDS_REBUILD" class="pull-right">
            <a href="#/build">System needs a rebuild</a>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="row-fluid">
    <div class="sidebar-nav">
      <ul class="side-menu" ng-controller="mainMenuCtrl">
        <li ng-show="INSTALLED_FEATURES > 0" ng-class="{true:'active', false:''}[isPageSelected('#/installed-features')]">
          <a href="#/installed-features">
            <i class="fa fa-gears"></i>
            <span>Installed features</span>
            <span ng-show="TOTAL_NEEDS_ATTENTION > 0" class="badge pull-right alert-success">{{TOTAL_NEEDS_ATTENTION}}</span>
          </a>
        </li>
        <li ng-class="{true:'active', false:''}[isPageSelected('#/library')]">
          <a href="#/library">
            <i class="fa fa-gear"></i>
            <span>Library</span>
<!--            <span class="badge pull-right alert-info">{{TOTAL_PLUGINS}}</span> -->
          </a>

        </li>
        <li ng-class="{true:'active', false:''}[isPageSelected('#/tools')]">
          <a href="#/tools">
            <i class="fa fa-gavel"></i>
            <span>Tools</span>
<!--            <span class="badge  pull-right alert-info">{{TOTAL_TOOLS}}</span> -->
          </a>
        </li>
        <li ng-class="{true:'active', false:''}[isPageSelected('#/build')]">
          <a href="#/build">
            <i ng-hide="NEEDS_REBUILD" class="fa fa-check-square-o"></i>
            <i ng-show="NEEDS_REBUILD" class="fa fa-exclamation-triangle"></i>
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
            <i class="fa fa-external-link"></i>
            <span>Feedback</span></a>
        </li>
      </ul>
    </div>
    <div class="content fixed-fixed">
      <div class="row-fluid">
        <div class="col-lg-12" ui-view autoscroll="false">
        </div>
      </div>
    </div>
  </div>

</div>

<footer class="footer">
  <p><span class="project-version">version: ${project.version}</span></p>
</footer>

<!-- Include the loader.js script -->
<script src="${pageContext.request.contextPath}/js/loader.js" data-modules="http://localhost:8080/essentials/rest/plugins/modules"></script>

</body>
</html>