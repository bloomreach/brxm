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

<!DOCTYPE html>


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
      angular.module('myApp', ['ngRoute'])
        .config(function ($routeProvider) {

          $routeProvider
            .when('/', {
              template: '<h1>Hello from route and {{message}}</h1>',
              controller: 'myController'
            })
        })
        .controller('myController', function ($scope) {
          console.log("executed");
          $scope.message = 'From my controller';
        });

      // end module
    })();
  </script>

</head>
<body ng-app="myApp">
<div ng-controller="myController">
  <div>Expecting message: </div>
  <h1>{{message}}</h1>
  <div ng-view>
  </div>
</div>
<a href="index4.jsp">controllers</a>


</body>
</html>