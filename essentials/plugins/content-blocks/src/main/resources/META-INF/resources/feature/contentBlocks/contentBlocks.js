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
        .controller('contentBlocksCtrl', function ($scope, $http, essentialsRestService) {
            var restEndpoint = essentialsRestService.baseUrl + '/contentblocks';

            $scope.compounds = [];
            $scope.documentTypes = [];
            $scope.copyChoices = [];
            $scope.pickerTypes = [ 'links', 'dropdown' ];

            $scope.addField = function() {
                var field = {
                    name: '',
                    pickerType: $scope.pickerTypes[0],
                    compounds: []
                };
                $scope.selectedDocumentType.contentBlocksFields.push(field);
            };

            $scope.deleteField = function(field) {
                var index = $scope.selectedDocumentType.contentBlocksFields.indexOf(field);
                if (index > -1) {
                    $scope.selectedDocumentType.contentBlocksFields.splice(index, 1);
                }
            };

            $scope.copyField = function() {
                var newField = angular.copy($scope.copyChoice.field);
                delete newField.caption;
                delete newField.name;

                // revert deep-copying of compounds, we must refer to the exact same instance
                // for the pre-selection to work.
                newField.compounds = [];
                angular.forEach($scope.copyChoice.field.compounds, function(compound) {
                    newField.compounds.push(compound);
                });

                $scope.selectedDocumentType.contentBlocksFields.push(newField);
                if ($scope.copyChoices.length > 1) {
                    $scope.copyChoice = null;
                }
            };

            $scope.update = function () {
                // translate the content blocks field compounds back to compoundrefs
                angular.forEach($scope.documentTypes, function(docType) {
                    angular.forEach(docType.contentBlocksFields, function(field) {
                        field.compoundRefs = [];
                        angular.forEach(field.compounds, function(compound) {
                            field.compoundRefs.push(compound.id);
                        });
                        delete field.compounds;
                    });
                });

                $http.post(restEndpoint, $scope.documentTypes)
                  .success(function () {
                    initDocTypes($scope.documentTypes, true);
                }).error(function () {
                    initDocTypes($scope.documentTypes, false);
                });
            };

            $scope.reset = function() {
                $http.get(restEndpoint).success(function (data) {
                    initDocTypes(data, true);
                    $scope.up = true;
                });
            };

            $scope.selectedDocTypeNamespace = function() {
                if ($scope.selectedDocumentType) {
                    return $scope.selectedDocumentType.id.split(':')[0];
                }
                return null;
            };

            $scope.selectedDocTypeName = function() {
                if ($scope.selectedDocumentType) {
                    return $scope.selectedDocumentType.id.split(':')[1];
                }
                return null;
            };

            $scope.init = function () {
                $http.get(restEndpoint + '/compounds').success(function (data) {
                    $scope.compounds = data;

                    // create the compound map
                    $scope.compoundMap = {};
                    angular.forEach($scope.compounds, function (compound) {
                        $scope.compoundMap[compound.id] = compound;
                    });


                    $scope.reset();
                });
            };
            $scope.init();

            function initDocTypes(docTypes, setCaption) {
                // preserve docType selection if possible
                var selectedName;
                if ($scope.selectedDocumentType) {
                    selectedName = $scope.selectedDocumentType.name;
                    $scope.selectedDocumentType = null;
                }
                $scope.documentTypes = docTypes;
                $scope.copyChoices = [];

                // replace content blocks compound refs with actual compounds
                angular.forEach($scope.documentTypes, function(docType) {
                    angular.forEach(docType.contentBlocksFields, function(field) {
                        if (setCaption) {
                            field.caption = field.caption || field.name;
                        }
                        if (field.maxItems === 0) {
                            delete field.maxItems;
                        }
                        field.compounds = [];
                        angular.forEach(field.compoundRefs, function(compoundRef) {
                            field.compounds.push($scope.compoundMap[compoundRef]);
                        });

                        $scope.copyChoices.push({
                            "name": docType.name + ' - ' + field.name,
                            "field": field
                        });
                    });
                    if (docType.name === selectedName) {
                        $scope.selectedDocumentType = docType; // restore previous selection
                    }
                });

                // if there's only one document type, preselect it.
                if ($scope.documentTypes.length === 1) {
                    $scope.selectedDocumentType = $scope.documentTypes[0];
                }
                if ($scope.copyChoices.length === 1) {
                    $scope.copyChoice = $scope.copyChoices[0];
                }
            }
        })
})();