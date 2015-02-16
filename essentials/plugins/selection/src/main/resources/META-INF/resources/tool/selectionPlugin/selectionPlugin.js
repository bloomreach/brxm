/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
            var singlePresentations = [
                { id: 'dropdown', label: 'Dropdown'},
                { id: 'radioboxes', label: 'Radioboxes'}
            ];
            var multiplePresentations = [
                { id: 'selectlist', label: 'Select list' },
                { id: 'checkboxes', label: 'Checkboxes' },
                { id: 'palette', label: 'Palette' }
            ];

            $scope.addField = function() {
                var payload = {
                    values: {
                        namespace:     $scope.selectedDocumentType.prefix,
                        documentType:  $scope.selectedDocumentType.name,
                        fieldName:     $scope.fieldName,
                        selectionType: $scope.selectionType,
                        valueList:     $scope.selectedValueList.value,
                        presentation:  $scope.presentation.id,
                        orientation:   $scope.orientation,
                        maxRows:       $scope.maxRows,
                        allowOrdering: $scope.allowOrdering
                    }
                };
                $http.post(restEndpoint + 'addfield/', payload).success(function () {
                    resetAddFieldForm();
                    reloadSelectionFields($scope.selectedDocumentType);
                    $scope.fieldAdded = true;
                    $scope.modifiedType = $scope.selectedDocumentType;
                });
            };
            $scope.showDocument = function(documentType) { // don't show the basedocument option
                return documentType.name !== 'basedocument';
            };

            $scope.valueListAsOption = function(valueList) {
                return valueList.key + ' (' + valueList.value + ')';
            };
            $scope.selectionTypes = [ 'single', 'multiple' ];
            $scope.$watch('selectionType', function(newValue) {
                console.log("Selection type: ", newValue);
                if (newValue === 'single') {
                    $scope.typePresentations = singlePresentations;
                } else if (newValue === 'multiple') {
                    $scope.typePresentations = multiplePresentations;
                }
                $scope.presentation = $scope.typePresentations[0]; // set default
            });
            $scope.orientations = [ 'vertical', 'horizontal' ];
            $scope.orientation = $scope.orientations[0]; // set default
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

            $scope.documentTypes = [];
            $http.get($rootScope.REST.documents).success(function (data){
                $scope.documentTypes = data;

                // if there's only one selectable type, preselect it.
                var selectable = [];
                angular.forEach($scope.documentTypes, function(type) {
                    if ($scope.showDocument(type)) {
                        selectable.push(type);
                    }
                });
                if (selectable.length == 1) {
                    $scope.selectedDocumentType = selectable[0];
                }
            });

            // when changing the document type, set the default position and retrieve a fresh list of fields
            $scope.$watch('selectedDocumentType', function (newDocType) {
                if (newDocType) {
                    reloadSelectionFields(newDocType);
                } else {
                    $scope.selectionFields = [];
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
                console.log("Setting selection type");
                $scope.selectionType = 'single';
                $scope.selectedValueList = undefined;
                $scope.allowOrdering = false;
                $scope.maxRows = undefined;
            }
            function reloadSelectionFields(documentType) {
                $scope.selectionFields = [];
                $http.get(restEndpoint + 'fieldsfor/' + documentType.fullName).success(function (data) {
                    $scope.selectionFields = data;
                });
            }
        })
})();
