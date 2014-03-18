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

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.AddMenuItemCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            'hippo.channel.FeedbackService',
            'hippo.channel.FormValidationService',
            'hippo.channel.menu.MenuService',
            function ($scope, $state, $stateParams, FeedbackService, FormValidationService, MenuService) {
                var parentItemId = $stateParams.menuItemId;

                // the user should submit or cancel before closing the menu
                FormValidationService.setValidity(false);

                $scope.selectedMenuItem = {
                    linkType: 'SITEMAPITEM',
                    title: '',
                    link: ''
                };

                $scope.submit = function() {
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

                        MenuService.createMenuItem(parentItemId, $scope.selectedMenuItem, {
                            position: position,
                            siblingId: siblingId
                        }).then(
                            function (menuItemId) {
                                FormValidationService.setValidity(true);

                                $state.go('menu-item.edit', {
                                    menuItemId: menuItemId
                                });
                            },
                            function (errorResponse) {
                                $scope.feedback = FeedbackService.getFeedback(errorResponse);
                            }
                        );
                    }
                };

                $scope.dismissFeedback = function () {
                    $scope.feedback.message = '';
                };
                
                $scope.cancel = function () {
                    FormValidationService.setValidity(true);

                    $state.go('menu-item.edit', {
                        menuItemId: parentItemId
                    });
                };
            }
        ]);
}());
