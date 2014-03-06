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

    angular.module('hippo.channelManager.menuManager')

        .controller('hippo.channelManager.menuManager.TreeCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            '$rootScope',
            '$log',
            'hippo.channelManager.ConfigService',
            'hippo.channelManager.FeedbackService',
            'hippo.channelManager.menuManager.MenuService',
            function ($scope, $state, $stateParams, $rootScope, $log, ConfigService, FeedbackService, MenuService) {
                $scope.callbacks = {
                    itemClicked: function (itemScope) {
                        MenuService.saveMenuItem($scope.$parent.selectedMenuItem).then(function () {
                                $state.go('menu-item.edit', {menuItemId: itemScope.id});
                                $scope.$parent.selectedMenuItem = itemScope;
                            },
                            function (errorResponse) {}
                        );
                    },
                    itemMoved: function (sourceScope, modelData, sourceIndex, destScope, destIndex) {
                        var parentData = destScope.parentItemScope();
                        var destId = (!parentData) ? ConfigService.menuId : parentData.itemData().id;
                        MenuService.moveMenuItem(modelData.id, destId, destIndex).then(
                            function (message) {},
                            function (errorResponse) {
                                $scope.$parent.feedback = FeedbackService.getFeedback(errorResponse);

                                // move the item back at it's original place in the DOM
                                var removedItem = destScope.sortableModelValue.splice(destIndex, 1)[0];
                                sourceScope.sortableModelValue.splice(sourceIndex, 0, removedItem);
                            }
                        );

                        // prevent move action when the edit menu item form is invalid
                        MenuService.saveMenuItem($scope.$parent.selectedMenuItem).then(function () {},
                            function () {
                                var removedItem = destScope.sortableModelValue.splice(destIndex, 1)[0];
                                sourceScope.sortableModelValue.splice(sourceIndex, 0, removedItem);
                            }
                        );
                    },
                    orderChanged: function (scope, modelData, sourceIndex, destIndex) {
                        var parentData = scope.parentItemScope();
                        var destId = (!parentData) ? ConfigService.menuId : parentData.itemData().id;
                        MenuService.moveMenuItem(modelData.id, destId, destIndex).then(
                            function (message) {},
                            function (errorResponse) {
                            $scope.$parent.feedback = FeedbackService.getFeedback(errorResponse);
                            var removedItem = scope.sortableModelValue.splice(destIndex, 1)[0];
                            scope.sortableModelValue.splice(sourceIndex, 0, removedItem);
                        });

                        // prevent order change when the edit menu item form is invalid
                        MenuService.saveMenuItem($scope.$parent.selectedMenuItem).then(function () {},
                            function () {
                                var removedItem = scope.sortableModelValue.splice(destIndex, 1)[0];
                                scope.sortableModelValue.splice(sourceIndex, 0, removedItem);
                            }
                        );
                    }
                };
            }
        ]);
}());
