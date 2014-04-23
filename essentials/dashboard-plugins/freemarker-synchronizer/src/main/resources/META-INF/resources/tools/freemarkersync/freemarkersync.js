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
            .controller('freemarkerSyncCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'freemarkersync/';
                $scope.writeAction = function (action) {
                    var map = Essentials.mapBuilder();
                    angular.forEach($scope.scriptNodes, function (value) {
                        console.log(value.selected);
                        if (value.selected) {
                            if (action == "file") {
                                map.put(value.displayValue, value.value);

                            } else {
                                if (value.filePath != null) {
                                    map.put(value.value, value.filePath);
                                }
                            }
                        }
                    });

                    $http.post($scope.endpoint + action, map).success(function (data) {
                        $scope.init();
                    });

                };
                $scope.saveToRepository = function () {
                    $scope.writeAction("repository");
                };
                $scope.saveToFile = function () {
                    $scope.writeAction("file");
                };
                $scope.init = function () {
                    var query = Essentials.queryBuilder("//hst:hst/hst:configurations//element(*, hst:template)[@hst:script]");
                    $http.post($rootScope.REST.jcrQuery, query).success(function (data) {
                        $scope.scriptNodes = [];
                        angular.forEach(data.nodes, function (value) {
                            var myValue = value.path;
                            var displayValue = myValue.replace("/hst:hst/hst:configurations/", "").replace("/hst:templates/", "/");
                            $scope.scriptNodes.push({"value": value.path, "displayValue": displayValue, "selected": false, "filePath": null});
                        });
                        // get files and match with above paths:
                        $http.get($scope.endpoint).success(function (data) {
                            angular.forEach($scope.scriptNodes, function (node) {
                                var fileTemplates = data.items;
                                angular.forEach(fileTemplates, function (value) {
                                    if (value && value.value) {
                                        var fileName = node.displayValue + ".ftl";
                                        var myValue = value.value;
                                        if (myValue.indexOf(fileName, value.value.length - fileName.length) !== -1) {
                                            node.filePath = myValue;
                                        }
                                    }
                                });

                            });
                        });

                    });





                };
                $scope.init();
            })
})();