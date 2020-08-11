/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
        .controller('templateQueryGeneratorCtrl', function ($scope, $http, essentialsRestService) {
            $scope.endpoint = essentialsRestService.baseUrl + '/templatequerygenerator';
            $scope.resultMessages = [];

            $scope.identity = angular.identity; // for sorting

            var loadTemplateQueries = function() {
              $http.get($scope.endpoint + "/templatequeries").then(function (response) {
                $scope.tplQueries = response.data;
              });
            };

            var generateTemplateQueries = function(parameters) {
              $http.post($scope.endpoint, parameters)
                .then(function () {
                  loadTemplateQueries();
                });
            };

            $scope.generateAllTemplateQueries = function () {
                generateTemplateQueries({
                    contentTypes: $scope.tplQueries.map(tplQuery => tplQuery.contentType),
                    scopes: ['DOCUMENT', 'FOLDER']
                });
            };

            $scope.generateDocumentTemplateQuery = function(contentType) {
              generateTemplateQueries({
                contentTypes: [contentType],
                scopes: ['DOCUMENT']
              });
            };

            $scope.generateFolderTemplateQuery = function(contentType) {
              generateTemplateQueries({
                contentTypes: [contentType],
                scopes: ['FOLDER']
              });
            };

          loadTemplateQueries();
        })
})();
