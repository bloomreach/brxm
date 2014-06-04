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
            // TODO fetch columns
            $scope.columns = {"items": [
                {"key": "Left column", "value": "left"},
                {"key": "Right column", "value": "right"}
            ]};
            $scope.deliberatelyTrustDangerousSnippet = $sce.trustAsHtml('<a target="_blank" href="http://content-blocks.forge.onehippo.org">Detailed documentation</a>');
            $scope.introMessage = "Content Blocks plugin provides the content/document editor an ability to add multiple pre-configured compound type blocks to a document. You can configure the available content blocks on per document type basis.";
            $scope.pluginName = "contentBlocksPlugin";
            $scope.pluginInstalled = true;
            $scope.payload = {"cbpayload": {"items": {"items": []}}};
            $scope.selection = [];
            $scope.providerInput = "";
            $scope.selectedItem = [];
            $scope.providers = [];
            $scope.baseCmsNamespaceUrl = "http://localhost:8080/cms?path=";
            $scope.baseConsoleNamespaceUrl = "http://localhost:8080/cms/console?path=";
            $scope.map = {};

            $scope.selectChange = function () {
                angular.forEach($scope.documentTypes, function (docType, key) {
                    docType.providers.items = [];
                    angular.forEach(docType.providers.ritems, function (providerItem, key) {
                        $log.info($scope.map[providerItem.key]);
                        $log.info(docType.providers.items.push($scope.map[providerItem.key]));
                    });
                });
            };

            $scope.onDeleteProvider = function (provider) {

                $http.delete($rootScope.REST.compoundsDelete + provider.key).success(function (data) {
                    // reload providers, we deleted one:
                    $scope.loadProviders();

                });


            };
            $scope.onAddProvider = function (docName) {
                $scope.providerInput = "";
                // TODO: put providers
                $http.put($rootScope.REST.compoundsCreate + docName, docName).success(function (data) {
                    // reload providers, we added new one:
                    $scope.loadProviders();
                    $scope.loadDocumentTypes();
                });


            };


            $scope.addProviderToDocType = function (prov, docName) {
                var index = $scope.documentTypes.indexOf(docName);
                //check if is empty
                if ($scope.documentTypes[index].providers == "") {
                    $scope.documentTypes[index].providers = {"items": []};
                }
                $scope.documentTypes[index].providers.items.push(prov);

            };
            $scope.removeProviderFromDocType = function (prov, docName) {
                var index = $scope.documentTypes.indexOf(docName);
                var providers = $scope.documentTypes[index].providers.items;
                var providerIndex = providers.indexOf(prov);
                $scope.documentTypes[index].providers.items.splice(providerIndex, 1);
            };
            $scope.installPlugin = function () {
                $http.get($rootScope.REST.pluginInstall + $scope.pluginId).success(function (data) {
                    $scope.installMessage = data.value;
                });
            };

            /**
             * called on document save
             */
            $scope.saveBlocksConfiguration = function () {
                $scope.payload = {"documentTypes": {"items": []}};
                $scope.payload.documentTypes.items = $scope.documentTypes;
                // delete ritems, path properties, not mapped on the backend:
                angular.forEach($scope.payload.documentTypes.items, function (value) {
                    if (value.providers && value.providers.ritems) {
                        delete value.providers.ritems;
                    }
                    var items = value.providers.items;
                    angular.forEach(items, function (v) {
                        if (v.path) {
                            delete v.path;
                        }
                    });


                });

                console.log($scope.payload);
                $http.post($rootScope.REST.contentblocksCreate, $scope.payload
                ).success(function (data) {
                        // ignore
                    });
            };

            $scope.toggleCheckBox = function (docName) {
                var index = $scope.selection.indexOf(docName);
                // check if  selected
                if (index > -1) {
                    $scope.selection.splice(index, 1);
                }
                else {
                    $scope.selection.push(docName);
                }
            };
            $scope.splitString = function (string, nb) {
                $scope.array = string.split(',');
                return $scope.result = $scope.array[nb];
            };
            $scope.loadProviders = function () {

                $http.get($rootScope.REST.compounds).success(function (data) {
                    $scope.providers = data.items;
                    angular.forEach($scope.providers, function (provider, key) {
                        $scope.map[provider.key] = provider;
                    });
                });

            };
            $scope.loadDocumentTypes = function () {
                $http.get($rootScope.REST.documentTypes).success(function (data) {
                    $scope.documentTypes = data.items;
                    angular.forEach($scope.documentTypes, function (docType) {
                        docType.providers.ritems = [];
                        angular.forEach(docType.providers.items, function (providerItem) {
                            docType.providers.ritems.push($scope.map[providerItem.key]);
                        });
                    });
                });

            };
            $scope.init = function () {
                // check if plugin is installed
                $http.get($rootScope.REST.pluginInstallState + $scope.pluginId).success(function (data) {
                    //{"installed":false,"pluginId":"contentBlocks","title":"Content Blocks Plugin"}
                    // TODO enable check:
                    $scope.pluginInstalled = true;
                    //$scope.pluginInstalled = data.installed;
                });
                $scope.loadProviders();
                $scope.loadDocumentTypes();

            };
            $scope.init();
        })
})();