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

            // TODO: Check if the use of the package_install service here is correct. We separated the setup phase
            // (handled by the dashboard) from the configuration phase (here), and if the setup phase used different
            // parameters from the global project settings (why would it?), this may go wrong. Needs to be tested.
            $scope.execute = function () {
                var payload = Essentials.addPayloadData("templateName", $scope.templateName, null);
                Essentials.addPayloadData("sampleData", $scope.sampleData, payload);
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

                $http.post($rootScope.REST.PLUGINS.setupById('blogPlugin'), payload).success(function (data) {
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
                // retrieve plugin data
                $http.get($rootScope.REST.PLUGINS.byId($scope.pluginId)).success(function (p) {
                    $scope.plugin = p;
                });

                $http.get($rootScope.REST.PROJECT.coordinates).success(function (data) {
                    var coordinates = Essentials.keyValueAsDict(data.items);
                    $scope.importConfig.blogsBasePath = '/content/documents/' + coordinates.namespace + '/blog';
                    $scope.importConfig.authorsBasePath = '/content/documents/' + coordinates.namespace + '/blog' + '/authors';
                    $scope.importConfig.projectNamespace = coordinates.namespace;

                });

                $http.get($rootScope.REST.PROJECT.settings).success(function (data) {
                    $scope.projectSettings = data;
                    // set some defaults
                    $scope.templateLanguage = data.templateLanguage;
                    $scope.useSamples = data.useSamples;
                });
            };

            $scope.init();
        })
})();