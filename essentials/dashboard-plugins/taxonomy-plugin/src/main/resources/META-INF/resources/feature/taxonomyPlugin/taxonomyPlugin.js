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
        .controller('taxonomyPluginCtrl', function ($scope, $filter, $sce, $log, $rootScope, $http) {
            $scope.pluginId = "taxonomyPlugin";
            var endpointDocuments = $rootScope.REST.documents;
            var endpointTaxonomy = $scope.endpoint = $rootScope.REST.dynamic + 'taxonomyplugin/';
            $scope.addDocuments = function () {
                var selectedDocumentNames = [];
                var selectedTaxonomyNames = [];
                var documents = $filter('filter')($scope.documentTypes, {checked: true});
                angular.forEach(documents, function (value) {
                    selectedDocumentNames.push(value.name);
                    selectedTaxonomyNames.push(value.selectedTaxonomy.name);
                });
                var payload = Essentials.addPayloadData("documents", selectedDocumentNames.join(','), null);
                Essentials.addPayloadData("taxonomies", selectedTaxonomyNames.join(','), payload);
                $http.post(endpointTaxonomy + 'add', payload).success(function () {
                    $scope.fieldsAdded = true;
                    updateDocumentTypes();
                });
            };

            $scope.addTaxonomy = function () {
                var taxonomy = {
                    name: $scope.taxonomyName,
                    locales: extractLocales($scope.locales)
                };
                $http.post(endpointTaxonomy + 'taxonomies/add', taxonomy).success(function () {
                    loadTaxonomies();
                });
            };

            $scope.haveTaxonomyFields = function() {
                return $scope.typesWithTaxonomyField > 0;
            };
            //############################################
            // INITIALIZE APP:
            //############################################
            $http.get(endpointDocuments).success(function (data) {
                // Filter out basedocument
                $scope.documentTypes = [];
                angular.forEach(data, function(docType) {
                    if (docType.name !== 'basedocument') {
                        $scope.documentTypes.push(docType);
                    }
                });

                updateDocumentTypes();
            });
            //
            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.plugin = plugin;
            });
            $scope.typesWithTaxonomyField = 0;
            $scope.fieldsAdded = false;
            $scope.taxonomies = [];
            loadTaxonomies();
            //############################################
            // HELPERS
            //############################################
            function loadTaxonomies() {
                $scope.locales = [
                    {name: "en"},
                    {name: "fr"},
                    {name: "de"},
                    {name: "es"},
                    {name: "it"},
                    {name: "nl"}
                ];
                $scope.taxonomyName = null;

                $http.get(endpointTaxonomy + "taxonomies").success(function (data) {
                    $scope.taxonomies = data;

                    angular.forEach(data, function (taxonomy) {
                        taxonomy.localesString = taxonomy.locales.join(', ');
                    });
                });
            }

            function updateDocumentTypes() {
                angular.forEach($scope.documentTypes, function (docType) {
                    // reset to avoid inadvertent double adding of taxonomies
                    delete docType.selectedTaxonomy;
                    docType.checked = false;
                    $scope.typesWithTaxonomyField = 0;

                    // update list of per-document used taxonomies
                    $http.get(endpointTaxonomy + 'taxonomies/' + docType.name).success(function (taxonomies) {
                        docType.taxonomies = taxonomies;
                        docType.taxonomiesString = taxonomies.join(', ');
                        docType.hasTaxonomyFields = !!docType.taxonomiesString;
                        if (!docType.taxonomiesString) {
                            docType.taxonomiesString = 'none';
                        } else {
                            $scope.typesWithTaxonomyField++;
                        }
                    });
                });
            }

            // Make string array of checked Locales
            function extractLocales(l) {
                var loc = $filter('filter')(l, {checked: true});
                var locales = [];
                angular.forEach(loc, function (value) {
                    locales.push(value.name);
                });
                return locales;
            }
        })
})();
