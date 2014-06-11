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
        .controller('selectionPluginCtrl', function ($scope, $filter, $sce, $log, $rootScope, $http) {
            $scope.pluginId = "selectionPlugin";
            $scope.$on('update-plugin-install-state', function(event, args) {
                if ($scope.pluginId === args.pluginId && $scope.plugin !== undefined) {
                    $scope.plugin.installState = args.state;
                }
            });

            var restEndpoint = $rootScope.REST.dynamic + 'selectionplugin/';
            $scope.tickle = function() {
                $http.post(restEndpoint, {}).success(function (data) {
                    alert('Hurray!');
                });
            };
            $scope.addField = function() {
                var position = $scope.fieldPosition;
                if ($scope.selectedDocumentType.fieldLocations.length < 2) {
                    position = $scope.selectedDocumentType.fieldLocations[0];
                }
                var payload = {
                    values: {
                        namespace: $scope.selectedDocumentType.prefix,
                        documentType: $scope.selectedDocumentType.name,
                        fieldName: $scope.fieldName,
                        fieldPosition: position,
                        selectionType: $scope.selectionType,
                        valueList: $scope.selectedValueList.value
                    }
                };
                $http.post(restEndpoint + 'addfield/', payload).success(function (data) {
                    resetAddFieldForm();
                    reloadSelectionFields($scope.selectedDocumentType);
                });
            };
            $scope.showDocument = function(documentType) {
                return documentType.name !== 'basedocument';
            };

            $scope.positionMap = {
                '${cluster.id}.right': 'right',
                '${cluster.id}.left' : 'left'
            };
            $scope.positionName = function(pos) {
                // content type service doesn't provide ".item" suffix, while namespace JCR access does...
                var suffix = '.item';
                if (pos.slice(-suffix.length) === suffix) {
                    pos = pos.substr(0, pos.length - suffix.length);
                }
                return $scope.positionMap[pos];
            };
            $scope.selectionTypes = [ 'single', 'multiple' ];
            $scope.valueListNameByPath = function(path) {
                var name;
                angular.forEach($scope.valueLists, function(entry, index) {
                    if (entry.value === path) {
                        name = entry.key;
                    }
                });
                return name;
            }
            resetAddFieldForm();

            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.plugin = plugin;
            });
            $http.get($rootScope.REST.documents).success(function (data){
                $scope.documentTypes = data;
            });
            loadValueLists();

            // when changing the document type, retrieve a fresh list of
            $scope.$watch('selectedDocumentType', function (newValue) {
                if (newValue) {
                    reloadSelectionFields(newValue);
                }
            }, true);

            function loadValueLists() {
                $http.get($rootScope.REST.documents + "selection:valuelist").success(function (data) {
                    $scope.valueLists = data;
                });
            }
            function resetAddFieldForm() {
                $scope.fieldName = '';
                $scope.fieldPosition = '${cluster.id}.right'; // default to adding selection fields in the right column
                $scope.selectionType = 'single';
                $scope.selectedValueList = undefined;
            }
            function reloadSelectionFields(documentType) {
                $scope.selectionFields = [];
                $http.get(restEndpoint + 'fieldsfor/' + documentType.fullName).success(function (data) {
                    $scope.selectionFields = data;
                });
            }
        })
})();
