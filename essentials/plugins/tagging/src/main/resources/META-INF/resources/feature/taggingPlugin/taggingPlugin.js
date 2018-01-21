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
        .controller('taggingPluginCtrl', function ($scope, $http, essentialsRestService, essentialsContentTypeService) {
            $scope.widgetCols = 20;
            $scope.widgetRows = 2;
            $scope.numberOfSuggestions = 10;
            $scope.fieldsAdded = false;

            $scope.addDocuments = function () {
                var configuration = {
                    jcrContentTypes: [],
                    parameters: {
                        numberOfSuggestions: $scope.numberOfSuggestions,
                        widgetRows: $scope.widgetRows,
                        widgetCols: $scope.widgetCols
                    }
                };
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.checked) {
                        configuration.jcrContentTypes.push(docType.fullName);
                    }
                });
                $http.post(essentialsRestService.baseUrl + '/taggingplugin', configuration).success(function () {
                    $scope.fieldsAdded = true;
                });
            };

            essentialsContentTypeService.getContentTypes().success(function (docTypes) {
                // Filter out basedocument
                $scope.documentTypes = [];
                angular.forEach(docTypes, function(docType) {
                    if (docType.name !== 'basedocument') {
                        $scope.documentTypes.push(docType);
                    }
                });
            });
        })
})();
