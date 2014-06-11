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
        .controller('documentWizardCtrl', function ($scope, $filter, $sce, $log, $modal, $rootScope, $http) {
            var endpoint = $rootScope.REST.dynamic + 'documentwizard/';
            var endpointQueries = $rootScope.REST.documents_template_queries;
            $scope.pluginId = "documentWizardPlugin";
            $scope.valueListPath = null;
            $scope.documentQuery = "new-document";
            $scope.selectedDocument = null;
            $scope.shortcutName = null;
            $scope.baseFolder = null;
            $scope.classificationType = null;
            $scope.classificationTypes = ["date", "list"];
            $scope.addOk = function () {

            };
            $scope.documentFirstSorting = function (keyValue) {
                return keyValue.key.indexOf('document') == -1 ? 1 : 0;
            };

            $scope.addCancel = function () {
                $scope.showDialog = false;
                console.log("cancel");
            };
            $scope.addWizard = function () {
                var payload = Essentials.addPayloadData("documentType", $scope.selectedDocument.fullName, null);
                Essentials.addPayloadData("classificationType", $scope.classificationType, payload);
                Essentials.addPayloadData("baseFolder", $scope.baseFolder, payload);
                Essentials.addPayloadData("shortcutName", $scope.shortcutName, payload);
                Essentials.addPayloadData("documentQuery", $scope.documentQuery, payload);
                Essentials.addPayloadData("valueListPath", $scope.valueListPath, payload);
                $http.post(endpoint, payload).success(function (data) {

                });

            };


            //############################################
            // INIT
            //############################################
            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
            });
            $http.get($rootScope.REST.documents).success(function (data) {
                $scope.documentTypes = data;
            });

            $http.get(endpointQueries).success(function (data) {
                $scope.queries = data;
            });

            $http.get($rootScope.REST.documents + "selection:valuelist").success(function (data) {
                $scope.valueLists = data;
                console.log("[]]]]]]]]]]]]]]");
                console.log(data);
            });


        })
})();
