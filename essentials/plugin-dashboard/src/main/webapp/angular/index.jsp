<!DOCTYPE html>
<!--
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
  -->

<html>
<head lang="en">
    <meta charset="UTF-8">
    <title>Demo</title>


    <script src="${pageContext.request.contextPath}/js/lib/angular.js"></script>
    <script src="${pageContext.request.contextPath}/js/lib/angular-route.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/lib/angular-ui-router.js"></script>

    <script type="application/javascript">
    (function () {
      "use strict";

      angular.module('myApp', [])
    })();
    </script>


</head>
<body ng-app="myApp">

<input type="text" ng-model="myText"/>
<h1>Hello {{myText}}</h1>
<h1>Hello <input type="text" ng-model="myText"/></h1>

<a href="index2.jsp">Directives</a>


</body>
</html>