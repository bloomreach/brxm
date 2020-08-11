/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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
        .controller('selectionPluginCtrl', function ($scope, $http, essentialsRestService, essentialsContentTypeService) {

            var restEndpoint = essentialsRestService.baseUrl + '/selectionplugin';
            var singlePresentations = [
                { id: 'dropdown', label: 'Dropdown'},
                { id: 'radioboxes', label: 'Radio Group'}
            ];
            var multiplePresentations = [
                { id: 'selectlist', label: 'Select List' },
                { id: 'checkboxes', label: 'Checkboxes' },
                { id: 'palette', label: 'Palette' }
            ];

            // Since the tabs use transclusive scopes, we need to put our two-way bound variables into data structure.
            $scope.data = {};
            $scope.initializing = true;

            $scope.addField = function() {
               var maxRows = isNaN(parseInt($scope.data.maxRows)) ? 10: parseInt($scope.data.maxRows);
                var payload = {
                    jcrContentType: $scope.data.selectedDocumentType.fullName,
                    fieldName:      $scope.data.fieldName,
                    selectionType:  $scope.data.selectionType,
                    valueList:      $scope.data.selectedValueList.jcrPath,
                    presentation:   $scope.data.presentation.id,
                    orientation:    $scope.data.orientation,
                    maxRows:        maxRows,
                    allowOrdering:  $scope.data.allowOrdering
                };
                $http.post(restEndpoint + '/addfield', payload).then(function () {
                    resetAddFieldForm();
                    reloadSelectionFields($scope.data.selectedDocumentType);
                    $scope.fieldAdded = true;
                    $scope.modifiedType = $scope.data.selectedDocumentType;
                });
            };
            $scope.showDocument = function(documentType) { // don't show the basedocument option
                return documentType.name !== 'basedocument';
            };

            $scope.valueListAsOption = function(valueList) {
                return valueList.displayName + ' (' + valueList.jcrPath + ')';
            };
            $scope.saveProvisioning = function() {
                var provisionedValueLists = [];
                angular.forEach($scope.provisionedValueLists, function(valueList) {
                    if (valueList.included) {
                        provisionedValueLists.push({
                            id: valueList.id,
                            path: valueList.path
                        });
                    }
                });
                $http.post(restEndpoint + '/spring', provisionedValueLists).then(function() {
                    loadProvisionedValueLists();
                });
            };

            $scope.selectionTypes = [ 'single', 'multiple' ];
            $scope.$watch('data.selectionType', function(newValue) {
                if (newValue === 'single') {
                    $scope.typePresentations = singlePresentations;
                } else if (newValue === 'multiple') {
                    $scope.typePresentations = multiplePresentations;
                }
                $scope.data.presentation = $scope.typePresentations[0]; // set default
            });
            $scope.orientations = [ 'vertical', 'horizontal' ];
            $scope.data.orientation = $scope.orientations[0]; // set default
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
            essentialsContentTypeService.getContentTypes().then(function (response){
                $scope.documentTypes = response.data;
                $scope.initializing = false;

                // if there's only one selectable type, preselect it.
                var selectable = [];
                angular.forEach($scope.documentTypes, function(type) {
                    if ($scope.showDocument(type)) {
                        selectable.push(type);
                    }
                });
                if (selectable.length == 1) {
                    $scope.data.selectedDocumentType = selectable[0];
                }
            });

            // when changing the document type, set the default position and retrieve a fresh list of fields
            $scope.$watch('data.selectedDocumentType', function (newDocType) {
                if (newDocType) {
                    reloadSelectionFields(newDocType);
                } else {
                    $scope.selectionFields = [];
                }
            }, true);

            // Helper functions
            function loadValueLists() {
                essentialsContentTypeService.getContentTypeInstances('selection:valuelist').then(function (response) {
                    $scope.valueLists = response.data;

                    loadProvisionedValueLists();
                });
            }
            function loadProvisionedValueLists() {
                if ($scope.valueLists.length > 0) {
                    $http.get(restEndpoint + '/spring').then(function (response) {
                        var provisionedValueLists = [];
                        angular.forEach($scope.valueLists, function(valueList) {
                            var oldItem, newItem;
                            angular.forEach(response.data, function(oldValueList) {
                                if (oldValueList.path === valueList.jcrPath) {
                                    oldItem = oldValueList;
                                }
                            });
                            if (oldItem) {
                                oldItem.included = true;
                                newItem = oldItem;
                            } else {
                                newItem = {
                                    path: valueList.jcrPath
                                };
                            }
                            provisionedValueLists.push(newItem);
                        });
                        $scope.provisionedValueLists = provisionedValueLists;
                    });
                }
            }
            function resetAddFieldForm() {
                $scope.data.fieldName = '';
                $scope.data.selectionType = 'single';
                $scope.data.selectedValueList = undefined;
                $scope.data.allowOrdering = false;
                $scope.data.maxRows = 10;
            }
            function reloadSelectionFields(documentType) {
                $scope.selectionFields = [];
                $http.get(restEndpoint + '/fieldsfor/' + documentType.fullName).then(function (response) {
                    $scope.selectionFields = response.data;
                });
            }
        })
})();
