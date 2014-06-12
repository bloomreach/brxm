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

                var restEndpoint = $rootScope.REST.dynamic + 'selectionplugin/';

                // Initialize the controller scope
                $scope.pluginId = "selectionPlugin";
                $scope.$on('update-plugin-install-state', function(event, args) {
                    if ($scope.pluginId === args.pluginId && $scope.plugin !== undefined) {
                        $scope.plugin.installState = args.state;
                    }
                });

                $scope.addField = function() {
                    var payload = {
                        values: {
                            namespace:     $scope.selectedDocumentType.prefix,
                            documentType:  $scope.selectedDocumentType.name,
                            fieldName:     $scope.fieldName,
                            fieldPosition: $scope.fieldPosition,
                            selectionType: $scope.selectionType,
                            valueList:     $scope.selectedValueList.value
                        }
                    };
                    $http.post(restEndpoint + 'addfield/', payload).success(function () {
                        resetAddFieldForm();
                        reloadSelectionFields($scope.selectedDocumentType);
                        $scope.regenerateBeans = true;
                    });
                };
                $scope.showDocument = function(documentType) { // don't show the basedocument option
                    return documentType.name !== 'basedocument';
                };

                $scope.positionMap = {
                    '${cluster.id}.right.item': 'right',
                    '${cluster.id}.left.item' : 'left'
                };
                $scope.valueListAsOption = function(valueList) {
                    return valueList.key + ' (' + valueList.value + ')';
                };
                $scope.selectionTypes = [ 'single', 'multiple' ];
                $scope.valueListNameByPath = function(path) {
                    var name = '';
                    angular.forEach($scope.valueLists, function(entry) {
                        if (entry.value === path) {
                            name = entry.key;
                        }
                    });
                    return name;
                };
                resetAddFieldForm();
                loadValueLists();

                $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                    $scope.plugin = plugin;
                });
                $http.get($rootScope.REST.documents).success(function (data){
                    $scope.documentTypes = data;
                });

                // when changing the document type, set the default position and retrieve a fresh list of fields
                $scope.$watch('selectedDocumentType', function (newDocType) {
                    if (newDocType) {
                        $scope.positionMatters = newDocType.fieldLocations.length > 1;
                        $scope.fieldPosition = newDocType.fieldLocations[0];
                        reloadSelectionFields(newDocType);
                    }
                }, true);

                // Helper functions
                function loadValueLists() {
                    $http.get($rootScope.REST.documents + "selection:valuelist").success(function (data) {
                        $scope.valueLists = data;
                    });
                }
                function resetAddFieldForm() {
                    $scope.fieldName = '';
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
