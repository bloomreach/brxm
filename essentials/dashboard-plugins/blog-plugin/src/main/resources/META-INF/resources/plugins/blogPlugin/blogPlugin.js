/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
        .controller('blogPluginCtrl', function ($scope, $sce, $log, $rootScope, $http) {
            $scope.templateName = 'jsp';
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
            $scope.pluginId = "blogPlugin";
            $scope.sampleData = true;
            $scope.templateName = 'jsp';
            $scope.payload = {values: {pluginId: $scope.pluginId, sampleData: $scope.sampleData, templateName: $scope.templateName}};
            $scope.$watchCollection("[sampleData, templateName]", function () {
                $scope.payload = {values: {pluginId: $scope.pluginId, sampleData: $scope.sampleData, templateName: $scope.templateName}};
            });
            $scope.run = function () {
                $http.post($rootScope.REST.package_install, $scope.payload).success(function (data) {
                });
            };


            $scope.execute = function () {
                var payload = Essentials.addPayloadData("templateName", $scope.templateName, null);
                Essentials.addPayloadData("sampleData", $scope.sampleData, payload);
                Essentials.addPayloadData("pluginId", "blogPlugin", payload);
                if ($scope.setupImport) {
                    // prefix importer values, so we have no key clashes:
                    var prefix = "importer_";
                    Essentials.addPayloadData('importer_setupImport', true, payload);
                    for (var key in $scope.importConfig) {
                        if ($scope.importConfig.hasOwnProperty(key)) {
                            var value = $scope.importConfig[key];
                            if (key == 'urls') {
                                var suffix = 0;
                                angular.forEach(value, function (val) {
                                    suffix++;
                                    var v = val.value;
                                    var author = val.author;
                                    console.log(author);
                                    var k = 'url' + suffix;
                                    var keyAuthor = 'author' + suffix;
                                    Essentials.addPayloadData(prefix + k, v, payload);
                                    Essentials.addPayloadData(prefix + keyAuthor, author, payload);

                                });
                            } else {
                                Essentials.addPayloadData(prefix + key, value, payload);
                            }

                        }
                    }
                }

                $http.post($rootScope.REST.package_install, payload).success(function (data) {
                    // globally handled
                });
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
                $http.get($rootScope.REST.projectSettings).success(function (data) {
                    $rootScope.projectSettings = Essentials.keyValueAsDict(data.items);
                    $scope.projectSettings = data;
                    $scope.importConfig.blogsBasePath = '/content/documents/' + $rootScope.projectSettings.namespace + '/blog';
                    $scope.importConfig.authorsBasePath = '/content/documents/' + $rootScope.projectSettings.namespace + '/blog' + '/authors';
                    $scope.importConfig.projectNamespace = $rootScope.projectSettings.namespace;
                });

            };
            $scope.init();
        })
})();