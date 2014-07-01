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
        .controller('restServicesCtrl', function ($scope, $sce, $log, $rootScope, $http) {
            $scope.endpoint = $rootScope.REST.dynamic + 'restservices/';
            $scope.resultMessages = [];
            $scope.restType = null;
            $scope.selectedType = "Choose REST type";
            $scope.restName = "restapi";
            $scope.showDocuments = false;
            $scope.documentTypes = [];

            $scope.init = function () {

            };
            $scope.onChangeType = function (restType) {
                $scope.restType = restType;
                if (restType == 'plain') {
                    // fetch documents:
                    $http.get($rootScope.REST.documents).success(function (data) {
                        $scope.documentTypes = data;
                    });
                } else {
                    $scope.documentTypes = [];
                }
            };
            $scope.checked = function () {
                var valid = checkValid();
                if (!valid) {
                } else {
                    $scope.restForm.myDocument.$setValidity("checked", true);
                }
            };
            $scope.runRestSetup = function () {
                // check if we have selected documents:
                if ($scope.restType == 'plain') {
                    var valid = checkValid();
                    if (!valid) {
                        $scope.restForm.myDocument.$setValidity("checked", false);
                        displayError("Please select at least one document type");
                        return;
                    }
                }

                var files = [];
                angular.forEach($scope.documentTypes, function (value) {
                    if (value.checked && value.fullPath) {
                        files.push(value.fullPath);
                    }
                });
                var fileString = files.join(',');
                var payload = Essentials.addPayloadData("restName", $scope.restName, null);
                Essentials.addPayloadData("restType", $scope.restType, payload);
                Essentials.addPayloadData("javaFiles", fileString, payload);
                $http.post($scope.endpoint, payload).success(function (data) {
                    // TODO: display reboot message and instruction sets executed
                });
            };
            $scope.init();
            //############################################
            // UTIL
            //############################################
            function displayError(msg) {
                $rootScope.globalError = [];
                $rootScope.feedbackMessages = [];
                $rootScope.globalError.push(msg);
            }

            function checkValid() {
                var valid = false;
                angular.forEach($scope.documentTypes, function (value) {
                    if (value.checked) {
                        valid = true;
                    }
                });
                return valid;
            }

        })
})();