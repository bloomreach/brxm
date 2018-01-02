/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
        .controller('relatedDocumentsCtrl', function ($scope, $rootScope, $http) {
            var endpoint = $rootScope.REST.dynamic + 'related-documents';
            $scope.pluginId = "relatedDocumentsPlugin";
            $scope.fieldsAdded = false;
            $scope.endpoint = $rootScope.REST.root + '/jcrbrowser/folders';
            $scope.addDocs = function () {
                var configuration = {
                    fields: []
                };
                angular.forEach($scope.documentTypes, function (value) {
                    if (value.checked) {
                        configuration.fields.push({
                            jcrContentType: value.fullName,
                            searchPath: value.searchPaths,
                            nrOfSuggestions: value.numberOfSuggestions
                        });
                    }
                });
                $http.post(endpoint, configuration).success(function () {
                    $scope.fieldsAdded = true;
                });
            };

            //############################################
            // INIT
            //############################################
            $http.get($rootScope.REST.PLUGINS.byId($scope.pluginId)).success(function (plugin) {
                $scope.plugin = plugin;
            });

            $http.get($rootScope.REST.documents).success(function (docTypes) {
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
