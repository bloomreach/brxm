/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

    angular.module('hippo.channel.menu')
        .controller('hippo.channel.menu.PickerCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            'hippo.channel.menu.PickerService',
            'hippo.channel.menu.MenuService',
            function ($scope, $state, $stateParams, PickerService, MenuService) {
                $scope.selectDocument = function() {
                    $state.go('menu-item.edit', {
                        menuItemId: $stateParams.menuItemId,
                        selectedDocumentPath: $scope.selectedDocument.pathInfo
                    });
                };
                $scope.cancelPicker = function() {
                    $state.go('menu-item.edit', {
                        menuItemId: $stateParams.menuItemId
                    });
                };
                $scope.treeItems = PickerService.getTree();
                $scope.pickerTypes = [
                    {
                        name: 'Documents'
                    }
                ];
                $scope.pickerType = $scope.pickerTypes[0];
                $scope.selectedDocument = null;

                var menuData = MenuService.getMenuData();

                if($stateParams.link) {
                    PickerService.getInitialData(menuData.siteContentIdentifier, $stateParams.link).then(function() {
                        navigateToSelected($scope.treeItems);
                        function navigateToSelected(items, parent) {
                            angular.forEach(items, function (item) {
                                if (item.selected) {
                                    $state.go('picker.docs', {
                                        pickerTreeItemId: parent.id
                                    });
                                    $scope.selectedItem = parent;
                                    $scope.selectedDocument = item;
                                }
                                if(item.items) {
                                    navigateToSelected(item.items, item);
                                }
                            });
                        }
                    });
                } else if(angular.isArray(menuData.items)) {
                    PickerService.getInitialData(menuData.siteContentIdentifier);
                } else {
                    MenuService.getMenu().then(
                        function(menuData){
                            PickerService.getInitialData(menuData.siteContentIdentifier);
                        }
                    );
                }

            }
        ]);
}());
