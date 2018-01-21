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
        .controller('restServicesCtrl', function ($scope, $http, essentialsRestService, essentialsContentTypeService) {
            $scope.endpoint = essentialsRestService.baseUrl + '/restservices';
            $scope.genericRestName = "api";
            $scope.manualRestName = "api-manual";
            $scope.documentTypes = [];
            $scope.checkedDocuments = 0;
            $scope.isGenericContentRestApiEnabled = true;
            $scope.isManualRestResourcesEnabled = false;

            $scope.onChangeManualRestName = function () {
                updateManualEndPoints();
            };
            $scope.checked = function (doc) {
                updateCheckedDocuments();
                return doc.checked;
            };
            $scope.isDataValid = function() {
                if (!$scope.isGenericContentRestApiEnabled && !$scope.isManualRestResourcesEnabled) {
                    return false; // need to enable at least one mount.
                }
                if ($scope.isGenericContentRestApiEnabled && $scope.isManualRestResourcesEnabled
                    && $scope.genericRestName === $scope.manualRestName) {
                    return false; // mount names must be mutually exclusive.
                }
                if ($scope.isGenericContentRestApiEnabled && $scope.genericRestForm.$invalid) {
                    return false; // invalid data for generic REST api.
                }
                if ($scope.isManualRestResourcesEnabled
                    && ($scope.manualRestForm.$invalid || $scope.checkedDocuments === 0)) {
                    return false; // invalid data for manual REST api.
                }
                return true;
            };
            $scope.runRestSetup = function () {
                var parameters = {
                    genericApiEnabled: $scope.isGenericContentRestApiEnabled,
                    genericRestName: $scope.genericRestName,
                    manualApiEnabled: $scope.isManualRestResourcesEnabled,
                    manualRestName: $scope.manualRestName,
                    javaFiles: []
                };
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.checked && docType.fullPath) {
                        parameters.javaFiles.push(docType.fullPath);
                    }
                });
                $http.post($scope.endpoint, parameters).success(function () {
                    // empty
                });
            };

            essentialsContentTypeService.getContentTypes().success(function (data) {
                angular.forEach(data, function (docType) {
                    if (docType.fullPath) {
                        $scope.documentTypes.push(docType);
                    }
                });
                updateManualEndPoints();
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
            function updateManualEndPoints() {
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.javaName) {
                        if ($scope.manualRestName) {
                            docType.endpoint = "http://localhost:8080/site/"
                            + $scope.manualRestName + "/" + docType.javaName.split('.')[0] + '/';
                        } else {
                            delete docType.endpoint;
                        }
                    }
                });
            }
        })
})();