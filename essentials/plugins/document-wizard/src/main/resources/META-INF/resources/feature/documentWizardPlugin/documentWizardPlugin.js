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
        .controller('documentWizardCtrl', function ($scope, $http, essentialsRestService, essentialsContentTypeService) {
            var endpoint = essentialsRestService.baseUrl + '/documentwizard';
            $scope.valueList = null;
            $scope.documentQuery = null;
            $scope.selectedDocument = null;
            $scope.shortcutName = null;
            $scope.baseFolder = null;
            $scope.classificationType = null;
            $scope.classificationTypes = [ "date", "list" ];
            $scope.shortcutLinkLabel = "New document";
            $scope.nameLabel = "New document";
            $scope.dateLabel = "Document date";
            $scope.listLabel = "";

            $scope.anyOf = function () {
                return true;
            };
            $scope.documentFirstSorting = function (query) {
                return query.name.indexOf('document') == -1 ? 1 : 0;
            };

            $scope.addCancel = function () {
                $scope.showDialog = false;
            };
            $scope.addWizard = function () {
                var payload = {
                  shortcutName: $scope.shortcutName,
                  classificationType: $scope.classificationType,
                  documentType: $scope.selectedDocument.fullName,
                  baseFolder: $scope.baseFolder,
                  documentQuery: $scope.documentQuery.name,

                  shortcutLinkLabel: $scope.shortcutLinkLabel,
                  nameLabel: $scope.nameLabel,
                  dateLabel: $scope.dateLabel,
                  listLabel: $scope.listLabel
                };
                
                if ($scope.valueList !== null) {
                    payload.valueListPath = $scope.valueList.jcrPath;
                }
                $http.post(endpoint, payload); // User feedback is handled globally
            };

            //############################################
            // INIT
            //############################################
            essentialsContentTypeService.getContentTypes().success(function (data) {
                $scope.documentTypes = data;
            });

            essentialsContentTypeService.getTemplateQueries().success(function (data) {
                $scope.queries = data;

                // Set default selection
                angular.forEach($scope.queries, function(query) {
                    if (query.name === "new-document") {
                        $scope.documentQuery = query;
                    }
                });
            });

            essentialsContentTypeService.getContentTypeInstances('selection:valuelist').success(function (data) {
                $scope.valueLists = data;
            });
        })
})();
