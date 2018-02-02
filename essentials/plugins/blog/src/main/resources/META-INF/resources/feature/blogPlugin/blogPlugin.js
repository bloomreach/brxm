/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    "use strict";
    angular.module('hippo.essentials')
        .controller('blogPluginCtrl', function ($scope, $http, essentialsRestService, essentialsPluginService, essentialsProjectService) {
            $scope.pluginId = "blogPlugin";
            $scope.setupImport = false;
            $scope.importConfig = {
                'active': true,
                'cronExpression': '0 0 6 ? * SUN',
                'cronExpressionDescription': 'Fires @ 6am on every sunday, More info @ http://www.quartz-scheduler.org/',
                'maxDescriptionLength': 300,
                'authorsBasePath': '/',
                urls: [
                    {'value': '', 'author': ''}
                ]
            };

            $scope.configure = function () {
                $http.post(essentialsRestService.baseUrl + '/blog', $scope.importConfig); // user feedback is handled globally
            };

            $scope.addUrl = function () {
                $scope.importConfig.urls.push({'value': ''});
            };
            $scope.removeUrl = function (url) {
                // we need at least on URL
                if ($scope.importConfig.urls.length <= 1) {
                    return;
                }
                var idx = $scope.importConfig.urls.indexOf(url);
                if (idx > -1) {
                    $scope.importConfig.urls.splice(idx, 1);
                }
            };
            $scope.init = function () {
                // retrieve plugin data
                essentialsPluginService.getPluginById($scope.pluginId).success(function (p) {
                    $scope.plugin = p;
                });

                essentialsProjectService.getProjectSettings().success(function (data) {
                    $scope.importConfig.blogsBasePath = '/content/documents/' + data.projectNamespace + '/blog';
                    $scope.importConfig.authorsBasePath = '/content/documents/' + data.projectNamespace + '/blog' + '/authors';
                    $scope.importConfig.projectNamespace = data.projectNamespace;
                });
            };

            $scope.init();
        })
})();