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
            var endpointTaxonomy = $scope.endpoint = $rootScope.REST.dynamic + '/taxonomy-plugin/';
            $scope.locales = [
                {name: "en"},
                {name: "fr"},
                {name: "de"},
                {name: "es"},
                {name: "it"},
                {name: "nl"}
            ];
            $scope.taxonomyName = null;
            $scope.selectChange = function () {
                console.log("Item changed");
            };
            $scope.run = function () {
                // we need at least one locale
                var documents = $filter('filter')($scope.documentTypes, {checked: true});
                var locales = $filter('filter')($scope.locales, {checked: true});
                if (locales.length == 0) {
                    locales.push("en");
                }
                var payload = Essentials.addPayloadData("locales", locales.join(','), null);
                Essentials.addPayloadData("locales", locales.join(','), null);
                Essentials.addPayloadData("documents", documents.join(','), payload);
                $http.post(endpointTaxonomy, payload).success(function (data) {
                    console.log(data);
                });
            };

            //############################################
            // INITIALIZE APP:
            //############################################

            $http.get(endpoint).success(function (data) {
                $scope.documentTypes = data;
            });
            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
            });


        })
})();
