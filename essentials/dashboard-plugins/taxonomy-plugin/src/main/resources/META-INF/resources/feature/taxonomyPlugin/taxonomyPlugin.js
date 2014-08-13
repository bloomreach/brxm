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
            var endpoint = $rootScope.REST.documents;
            var endpointTaxonomy = $scope.endpoint = $rootScope.REST.dynamic + 'taxonomyplugin/';
            var endpointDocument = endpointTaxonomy + "add";
            $scope.taxonomies = [];
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
                $http.post(endpointDocument, payload).success(function (data) {
                    //
                });
            };

            $scope.addTaxonomy = function () {
                var payload = Essentials.addPayloadData("locales", extractLocales($scope.locales), null);
                Essentials.addPayloadData("taxonomyName", $scope.taxonomyName, payload);
                $http.post(endpointTaxonomy, payload).success(function () {
                    loadTaxonomies();
                });
            };
            //############################################
            // INITIALIZE APP:
            //############################################
            $http.get(endpoint).success(function (data) {
                $scope.documentTypes = data;
            });
            //
            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.plugin = plugin;
            });
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

                $http.get(endpointTaxonomy + "taxonomies/").success(function (data) {
                    $scope.taxonomies = data;

                    angular.forEach(data, function (taxonomy) {
                        taxonomy.localesString = taxonomy.locales.join(', ');
                    });
                });
            }

            // Make CSV string of checked Locales
            function extractLocales(l) {
                var loc = $filter('filter')(l, {checked: true});
                var locales = [];
                if (loc.length == 0) {
                    locales.push('en');
                } else {
                    angular.forEach(loc, function (value) {
                        locales.push(value.name);
                    });
                }
                return locales.join(',');
            }

        })
})();
