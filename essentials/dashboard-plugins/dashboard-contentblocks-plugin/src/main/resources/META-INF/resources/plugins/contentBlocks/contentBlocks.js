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

            $scope.deliberatelyTrustDangerousSnippet = $sce.trustAsHtml('<a target="_blank" href="http://content-blocks.forge.onehippo.org">Detailed documentation</a>');
            $scope.introMessage = "Content Blocks plugin provides the content/document editor an ability to add multiple pre-configured compound type blocks to a document. You can configure the available content blocks on per document type basis.";
            $scope.providerInput = "";
            $scope.baseCmsNamespaceUrl = "http://localhost:8080/cms?path=";
            $scope.providers = [];
            $scope.providerMap = {};

            $scope.selectChange = function () {
                angular.forEach($scope.documentTypes, function (docType, key) {
                    docType.providers.items = [];
                    angular.forEach(docType.providers.providerNames, function (providerItem, key) {
                        $log.info($scope.providerMap[providerItem.key]);
                        $log.info(docType.providers.items.push($scope.providerMap[providerItem.key]));
                    });
                });
            };

            // delete a provider
            $scope.onDeleteProvider = function (provider) {
                $http.delete(restEndpoint + 'compounds/delete/' + provider.key).success(function (data) {
                    // reload providers, we deleted one:
                    loadProviders();
                });
            };

            // create a provider
            $scope.onAddProvider = function (providerName) {
                $scope.providerInput = "";
                $http.put(restEndpoint + 'compounds/create/' + providerName, providerName).success(function (data) {
                    // reload providers, we added new one:
                    loadProviders();
                });
            };

            /**
             * called on document save
             */
            $scope.saveBlocksConfiguration = function () {
                var payload = {"documentTypes": {"items": []}};
                payload.documentTypes.items = $scope.documentTypes;
                // delete providerNames, path properties, not mapped on the backend:
                angular.forEach(payload.documentTypes.items, function (docType) {
                    if (docType.providers && docType.providers.providerNames) {
                        delete docType.providers.providerNames;
                    }
                    var providers = docType.providers.items;
                    angular.forEach(providers, function (provider) {
                        if (provider.path) {
                            delete provider.path;
                        }
                    });
                });

                $http.post(restEndpoint + 'compounds/contentblocks/create', payload).success(function (data) {
                    // ignore
                });
            };

            // helper function for deep-linking into the CMS document type editor.
            $scope.splitString = function (string, nb) {
                $scope.array = string.split(',');
                return $scope.result = $scope.array[nb];
            };
            $scope.init = function () {
                loadProviders();
                loadDocumentTypes();

            };
            $scope.init();
                
            // Helper functions
            function loadProviders() {
                $http.get(restEndpoint + 'compounds').success(function (data) {
                    $scope.providers = data.items;
                    
                    // (re-)initialize the provider map
                    $scope.providerMap = {};
                    angular.forEach($scope.providers, function (provider, key) {
                        $scope.providerMap[provider.key] = provider;
                    });
                });
            }
            function loadDocumentTypes() {
                $http.get(restEndpoint).success(function (data) {
                    $scope.documentTypes = data.items;
                    angular.forEach($scope.documentTypes, function (docType) {
                        docType.providers.providerNames = [];
                        angular.forEach(docType.providers.items, function (provider) {
                            docType.providers.providerNames.push($scope.providerMap[provider.key]);
                        });
                    });
                });
            }
        })
})();