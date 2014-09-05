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
        .controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http) {
            var restEndpoint = $rootScope.REST.dynamic + 'contentblocks/';

            $scope.baseCmsNamespaceUrl = "http://localhost:8080/cms?path=";
            $scope.providers = [];
            $scope.providerMap = {};
            $scope.documentTypes = [];

            $scope.newProviderName = "";
            $scope.createProvider = function () {
                $http.put(restEndpoint + 'providers/' + $scope.newProviderName).success(function (data) {
                    loadCompounds(); // reload providers after adding one
                });
                $scope.newProviderName = "";
            };

            $scope.update = function () {
                // per document type, compare the current providers (which may have been adjusted)
                // with the originally loaded providers. If there are differences, some providers
                // will have to be added or removed.
                var actionableDocuments = [];
                angular.forEach($scope.documentTypes, function(docType) {
                    var actionableProviders = [];
                    angular.forEach(docType.providers, function(savedProvider) {
                        var isAdded = true;
                        angular.forEach(docType.providerActions, function(loadedProvider) {
                            if (savedProvider.name === loadedProvider.name) {
                                isAdded = false; // docType already had this provider.
                            }
                        });
                        if (isAdded) {
                            actionableProviders.push({
                                "name": savedProvider.name,
                                "add": true
                            });
                        }
                    });
                    angular.forEach(docType.providerActions, function(loadedProvider) {
                        var isDeleted = true;
                        angular.forEach(docType.providers, function(savedProvider) {
                            if (loadedProvider.name === savedProvider.name) {
                                isDeleted = false; // docType still has this provider
                            }
                        })
                        if (isDeleted) {
                            actionableProviders.push({
                                "name": loadedProvider.name,
                                "add": false
                            })
                        }
                    });
                    if (actionableProviders.length) {
                        actionableDocuments.push({
                            "name": docType.name,
                            "providerActions": actionableProviders
                        });
                    }
                });

                if (actionableDocuments.length) {
                    $http.post(restEndpoint + 'update', { "documentTypes": actionableDocuments }).success(function() {
                        loadDocumentTypes();
                    });
                }
            };

            $scope.init = function () {
                $http.get(restEndpoint + 'compounds').success(function (data) {
                    $scope.compounds = data;

                    // create the compound map
                    $scope.compoundMap = {};
                    angular.forEach($scope.compounds, function (compound) {
                        $scope.compoundMap[compound.id] = compound;
                    });

                    loadDocumentTypes();
                });
            };
            $scope.init();
                
            // Helper functions
            function loadCompounds() {
                // delete!
            }
            function loadDocumentTypes() {
                $http.get($rootScope.REST.documents_documents).success(function (data) {
                    $scope.documentTypes = data;

                    // filter document types to show
                    $scope.editable = [];
                    angular.forEach(data, function(type) {
                        if (type.name !== 'basedocument') {
                            $scope.editable.push(type);
                        }
                    });

                    // if there's only one editable type, preselect it.
                    if ($scope.editable.length == 1) {
                        $scope.selectedDocumentType = $scope.editable[0];
                    }
                });
            }
        })
})();