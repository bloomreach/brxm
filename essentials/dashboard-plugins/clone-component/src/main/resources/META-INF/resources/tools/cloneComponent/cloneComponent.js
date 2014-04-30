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
        .controller('cloneComponentCtrl', function ($scope, $sce, $log, $rootScope, $http) {
            $scope.endpoint = $rootScope.REST.dynamic + 'cloneComponent/';
            $scope.components = null;
            $scope.newComponents = [];
            $scope.run = function () {
            };
            $scope.validateName = function (name) {
                var valid = true;
                if (!name) {
                    return valid;
                }
                angular.forEach($scope.components, function (value) {
                    if (valid && (value.key == name)) {
                        valid = false;
                    }
                });
                if (valid) {
                    angular.forEach($scope.newComponents, function (value) {
                        if (valid && value.key == name) {
                            valid = false;
                        }
                    });
                }
                return valid;
            };
            $scope.validateTemplate = function (name) {
                var valid = true;
                if (!name) {
                    return valid;
                }
                angular.forEach($scope.components, function (value) {
                    if (valid && (value.value == name)) {
                        valid = false;
                    }
                });
                if (valid) {
                    angular.forEach($scope.newComponents, function (value) {
                        if (valid && value.value == name) {
                            valid = false;
                        }
                    });
                }
                return valid;
            };

            $scope.onComponentClick = function (component) {
                console.log(component);
            };
            $scope.checkShown = function (component) {
                console.log(component);
                return true;
            };
            $scope.validClones = function () {
                angular.forEach($scope.components, function (value, key) {
                    if (value.key && value.key.length > 0) {

                    }
                });
                return true;

            };
            $scope.init = function () {
                $http.get($scope.endpoint).success(function (data) {
                    $scope.components = data;
                    console.log("======================================");
                    console.log("======================================");
                    console.log("======================================");
                    console.log(data);
                    angular.forEach(data, function (value, key) {
                        $scope.newComponents.push({});
                    });

                });
            };
            $scope.init();
        })
})();