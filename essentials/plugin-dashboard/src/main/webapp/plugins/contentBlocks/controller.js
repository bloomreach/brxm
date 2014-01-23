(function () {
    "use strict";

    angular.module('hippo.essentials')
            .controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
                // TODO fetch columns
                $scope.columns = {"items": [
                    {"key": "Left column", "value": "left"},
                    {"key": "Right column", "value": "right"}
                ]};
                $scope.deliberatelyTrustDangerousSnippet = function () {
                    return $sce.trustAsHtml('<a target="_blank" href="http://content-blocks.forge.onehippo.org">Detailed documentation</a>');
                };
                $scope.introMessage = "Content Blocks plugin provides the content/document editor an ability to add multiple pre-configured compound type blocks to a document. You can configure the available content blocks on per document type basis.";
                $scope.pluginClass = "org.onehippo.cms7.essentials.dashboard.contentblocks.ContentBlocksPlugin";
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
                    $http.get($rootScope.REST.pluginInstall + $scope.pluginClass).success(function (data) {
                        $scope.installMessage = data.value;
                    });
                };

                /**
                 * called on document save
                 */
                $scope.saveBlocksConfiguration = function () {
                    $scope.payload = {"cbpayload": {"items": {"items": []}}};
                    $scope.payload.cbpayload.items.items = $scope.documentTypes;
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
                $scope.init = function () {
                    // check if plugin is installed
                    $http.get($rootScope.REST.pluginInstallState + $scope.pluginClass).success(function (data) {
                        //{"installed":false,"pluginLink":"contentBlocks","title":"Content Blocks Plugin"}
                        // TODO enable check:
                        $scope.pluginInstalled = true;
                        //$scope.pluginInstalled = data.installed;
                    });
                    $scope.loadProviders();
                    $http.get($rootScope.REST.documentTypes).success(function (data) {
                        $scope.documentTypes = data.items;
                        angular.forEach($scope.documentTypes, function (docType, key) {
                            docType.providers.ritems = [];
                            angular.forEach(docType.providers.items, function (providerItem, key) {
                                docType.providers.ritems.push($scope.map[providerItem.key]);
                            });
                        });
                    });
                };
                $scope.init();
            })
})();