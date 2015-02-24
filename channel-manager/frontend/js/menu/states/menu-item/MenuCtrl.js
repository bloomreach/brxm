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

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.MenuCtrl', [
            '$rootScope',
            '$scope',
            '$filter',
            '$state',
            '$stateParams',
            'hippo.channel.FeedbackService',
            'hippo.channel.FormStateService',
            'hippo.channel.menu.MenuService',
            function ($rootScope, $scope, $filter, $state, $stateParams, FeedbackService, FormStateService, MenuService) {
                $scope.isSaving = {};

                $scope.addItem = function () {
                    if (FormStateService.isDirty()) {
                        if (FormStateService.isValid()) {
                            MenuService.saveMenuItem($scope.$parent.selectedMenuItem).then(function() {
                                    addItemAfterCheckingValidity();
                                },
                                function (errorResponse) {
                                    $scope.feedback = FeedbackService.getFeedback(errorResponse);
                                });
                        }
                    } else {
                        addItemAfterCheckingValidity();
                    }

                    function addItemAfterCheckingValidity () {
                        $scope.isSaving.newItem = true;
                        MenuService.getMenu().then(function (menuData) {
                            var blankMenuItem = {
                                linkType: 'SITEMAPITEM',
                                title: $filter('incrementProperty')(menuData.items, 'title', $filter('translate')('UNTITLED'), 'items'),
                                link: ''
                            };

                            if ($stateParams.menuItemId) {
                                MenuService.getPathToMenuItem($stateParams.menuItemId).then(addMenuItemToPath);
                            } else {
                                addMenuItemToPath([]);
                            }

                            function addMenuItemToPath(path) {
                                var currentItem, parentItemId, position = MenuService.AFTER, siblingId;

                                // create child if currently selected item already has children.
                                // otherwise, create sibling.
                                if (path && path.length >= 1) {
                                    currentItem = path.pop();
                                    if (currentItem.items && currentItem.items.length > 0) {
                                        parentItemId = currentItem.id;
                                        position = MenuService.FIRST;
                                    } else if (path.length >= 1) {
                                        siblingId = currentItem.id;
                                        currentItem = path.pop();
                                        parentItemId = currentItem.id;
                                    }
                                }

                                MenuService.createMenuItem(parentItemId, blankMenuItem, {
                                    position: position,
                                    siblingId: siblingId
                                }).then(
                                    function (menuItemId) {
                                        FormStateService.setValid(true);

                                        if ($scope.$parent.selectedMenuItem) {
                                            $scope.$parent.selectedMenuItem.collapsed = false;
                                        }

                                        $state.go('menu-item.edit', {
                                            menuItemId: menuItemId
                                        });

                                        var deregisterListener = $scope.$on('menu-items-changed', afterItemIsAddedToList);
                                        function afterItemIsAddedToList() {
                                            $scope.isSaving.newItem = false;
                                            $rootScope.$broadcast('new-menu-item');
                                            deregisterListener();
                                        }
                                    },
                                    function (errorResponse) {
                                        $scope.isSaving.newItem = false;
                                        $scope.feedback = FeedbackService.getFeedback(errorResponse);
                                    }
                                );
                            }
                        });
                    }
                };
            }
        ]);
}());
