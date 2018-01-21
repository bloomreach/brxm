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
        .controller('taxonomyPluginCtrl', function ($scope, $http, essentialsRestService, essentialsContentTypeService) {
            var endpointTaxonomy = essentialsRestService.baseUrl + '/taxonomyplugin';
            $scope.addDocuments = function () {
                var taxonomyFields = [];
                angular.forEach($scope.documentTypes, function (docType) {
                    if (docType.checked) {
                        taxonomyFields.push({
                            jcrContentType: docType.fullName,
                            taxonomyName: docType.selectedTaxonomy.name
                        });
                    }
                });
                $http.post(endpointTaxonomy + '/add', taxonomyFields).success(function () {
                    $scope.fieldsAdded = true;
                    updateDocumentTypes();
                });
            };

            $scope.addTaxonomy = function () {
                var taxonomy = {
                    name: $scope.taxonomyName,
                    locales: extractLocales($scope.locales)
                };
                $http.post(endpointTaxonomy + '/taxonomies/add', taxonomy).success(function () {
                    loadTaxonomies();
                });
            };

            $scope.haveTaxonomyFields = function() {
                return $scope.typesWithTaxonomyField > 0;
            };
            //############################################
            // INITIALIZE APP:
            //############################################
            essentialsContentTypeService.getContentTypes().success(function (data) {
                // Filter out basedocument
                $scope.documentTypes = [];
                angular.forEach(data, function(docType) {
                    if (docType.name !== 'basedocument') {
                        $scope.documentTypes.push(docType);
                    }
                });

                updateDocumentTypes();
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

                $http.get(endpointTaxonomy + "/taxonomies").success(function (data) {
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
                    $http.get(endpointTaxonomy + '/taxonomies/' + docType.fullName).success(function (taxonomies) {
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
                var locales = [];
                angular.forEach(l, function (locale) {
                    if (locale.checked) {
                        locales.push(locale.name);
                    }
                });
                return locales;
            }
        })
})();
