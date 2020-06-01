<%@ page language="java" contentType="text/html; charset=UTF-8" session="false" pageEncoding="UTF-8" %>
<%--
  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)

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
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Essentials</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/components/hippo-theme/dist/css/main.css?v=${project.version}"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.css?v=${project.version}"/>
  <script type="application/javascript">
    window.SERVER_URL = '<%=request.getScheme()+"://"+request.getServerName()+':'+request.getServerPort()%>';
  </script>

  <%-- HIPPO THEME DEPS --%>
  <script src="${pageContext.request.contextPath}/components/jquery/dist/jquery.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/chosen-js/chosen.jquery.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular-animate/angular-animate.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular-aria/angular-aria.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular-chosen-localytics/dist/angular-chosen.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular-ui-bootstrap/dist/ui-bootstrap-tpls.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-ui-tree.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-theme/dist/js/main.js?v=${project.version}"></script>

  <%-- ESSENTIALS --%>
  <script src="${pageContext.request.contextPath}/components/angular-ui-router/release/angular-ui-router.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular-sanitize/angular-sanitize.js?v=${project.version}"></script>

  <script src="${pageContext.request.contextPath}/js/app.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/routes.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/directives.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/dashboard/api/services.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/dashboard/api/directives.js?v=${project.version}"></script>

  <link rel="icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
</head>
<body id="container"  ng-cloak>
<div class="hidden" ng-include="'${pageContext.request.contextPath}/components/hippo-theme/dist/images/hippo-icon-sprite.svg?v=${project.version}'"></div>
<essentials-notifier ng-show="feedbackMessages.length" messages="feedbackMessages" ng-class="{'log-visible':feedbackMessages.length && addLogClass}"></essentials-notifier>


<div class="hippo-navbar hippo-navbar-with-sidenav navbar navbar-default navbar-fixed-top" ng-controller="navbarCtrl" ng-hide="INTRODUCTION_DISPLAYED">
  <div class="container-fluid">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" ng-click="toggleCollapsed()">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <span ng-show="TOTAL_NEEDS_ATTENTION > 0" class="badge badge-primary notification-badge">{{TOTAL_NEEDS_ATTENTION}}</span>
      <a class="navbar-brand" href="${pageContext.request.contextPath}"
         title="version: ${project.version}">Essentials</a>
      <p class="navbar-text navbar-title">
        {{getPageTitle()}}
      </p>
      <div class="navbar-text hippo-navbar-icons">
        <a href="#/build" class="navbar-link">
          <hippo-icon name="refresh"></hippo-icon>
          <span class="hidden-xs">Rebuild</span>
          <hippo-icon name="bell" class="hi-color-danger" ng-show="NEEDS_REBUILD"></hippo-icon>
        </a>
        <a href="#" ng-click="showMessages($event)" ng-show="feedbackMessages.length && showMessagesNavbarLink"
           class="navbar-link" title="{{feedbackMessages.length}} notification message(s)">
          <hippo-icon name="info-circle" size="m"></hippo-icon><span class="badge badge-info">{{feedbackMessages.length}}</span>
        </a>
      </div>
    </div>

    <div class="navbar-collapse collapse hippo-sidenav ng-scope" ng-controller="mainMenuCtrl" uib-collapse="isCollapsed" ng-hide="INTRODUCTION_DISPLAYED">
      <ul class="nav navbar-nav" ng-hide="INTRODUCTION_DISPLAYED">
        <li ng-class="{true:'active', false:''}[isPageSelected('#/library')]">
          <a href="#/library">
            <hippo-icon name="shopping-cart" size="m"></hippo-icon>
           Library
          </a>
        </li>
        <li ng-show="INSTALLED_FEATURES > 0" ng-class="{true:'active', false:''}[isPageSelected('#/installed-features')]">
          <a href="#/installed-features">
            <hippo-icon name="arrow-fat-down-circle" size="m" class="hi-fill-white"></hippo-icon>
            Installed features&nbsp;&nbsp;
            <span ng-show="TOTAL_NEEDS_ATTENTION > 0" class="badge badge-danger" style="position:fixed">{{TOTAL_NEEDS_ATTENTION}}</span>
          </a>
        </li>
        <li ng-class="{true:'active', false:''}[isPageSelected('#/tools')]">
          <a href="#/tools">
            <hippo-icon name="wrench" size="m"></hippo-icon>
              Tools
          </a>
        </li>
        <li>
          <a href="#/introduction">
            <hippo-icon name="gear" size="m"></hippo-icon>
              Settings
          </a>
        </li>
        <li>
          <a target="_blank" href="https://issues.onehippo.com/rest/collectors/1.0/template/form/e07a17b7?os_authType=none">
            <hippo-icon name="comment" size="m"></hippo-icon>
            Feedback</a>
        </li>
      </ul>
    </div>
  </div>
</div>



<div class="main-content">
  <div class="container-fluid flex-column" ui-view>
    <h2>initializing...</h2>
  </div>
</div>
<!-- Include the loader.js script -->
<script src="${pageContext.request.contextPath}/js/loader.js" data-modules="<%=request.getScheme()+"://"+request.getServerName()+':'+request.getServerPort()%>/essentials/rest/plugins/modules"></script>


<style type="text/css">
  input.ng-invalid,
  input.ng-invalid-minlength {
    background-color: #e7484c;
    color: #fff;
  }

  select.ng-invalid + div.chosen-container {
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
    border: 2px solid #e7484c;
    -webkit-border-radius: 3px;
    -moz-border-radius: 3px;
    border-radius: 3px;
  }

  input:focus.ng-invalid,
  input:focus.ng-invalid-minlength {
    -webkit-box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075);
    border: 3px solid #e7484c;
  }

  input.ng-valid,
  input:focus.ng-valid {
    background-color: #fff;
    color: #000;
  }

  [ng\:cloak],
  [ng-cloak],
  [data-ng-cloak],
  [x-ng-cloak],
  .ng-cloak,
  .x-ng-cloak,
  .ng-hide {
    display: none !important;
  }

  ng\:form {
    display: block;
  }

  div.chosen-container[style] {
    min-width: 100px;
  }

  @-webkit-keyframes feedback_sequence {
    from {
      opacity: 0;
    }
    to {
      opacity: 1;
    }
  }

  @keyframes feedback_sequence {
    from {
      opacity: 0;
    }
    to {
      opacity: 1;
    }
  }
</style>
</body>
</html>
