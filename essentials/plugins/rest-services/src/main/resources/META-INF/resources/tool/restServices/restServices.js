/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
            $scope.restName = "";
            $scope.documentTypes = [];
            $scope.checkedDocuments = 0;

            $scope.onChangeRestName = function () {
                updateEndPoints();
            };
            $scope.checked = function (doc) {
                updateCheckedDocuments();
                return doc.checked;
            };
            $scope.runRestSetup = function () {
                // check if we have selected documents:
                var files = [];
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.checked && docType.fullPath) {
                        files.push(docType.fullPath);
                    }
                });
                var fileString = files.join(',');
                var payload = Essentials.addPayloadData("restName", $scope.restName, null);
                Essentials.addPayloadData("restType", "plain", payload);
                Essentials.addPayloadData("javaFiles", fileString, payload);
                $http.post($scope.endpoint, payload).success(function (data) {
                    // TODO: display reboot message and instruction sets executed
                });
            };

            $http.get($rootScope.REST.documents).success(function (data) {
                angular.forEach(data, function (docType) {
                    if (docType.fullPath) {
                        $scope.documentTypes.push(docType);
                    }
                });
                updateEndPoints();
            });

            //############################################
            // UTIL
            //############################################
            function updateCheckedDocuments() {
                var checkedDocuments = 0;
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.checked) {
                        checkedDocuments++;
                    }
                });
                $scope.checkedDocuments = checkedDocuments;
            }
            function updateEndPoints() {
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.javaName) {
                        if ($scope.restName) {
                            docType.endpoint = "http://localhost:8080/site/"
                            + $scope.restName + "/" + docType.javaName.split('.')[0] + '/';
                        } else {
                            delete docType.endpoint;
                        }
                    }
                });
            }
        })
})();