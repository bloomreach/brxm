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

    function moveItemModel(sourceScope, sourceIndex, destScope, destIndex) {
        var removedItem = destScope.sortableModelValue.splice(destIndex, 1)[0];
        sourceScope.sortableModelValue.splice(sourceIndex, 0, removedItem);
    }

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.TreeCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            '$rootScope',
            '$log',
            'hippo.channel.ConfigService',
            'hippo.channel.FeedbackService',
            'hippo.channel.FormStateService',
            'hippo.channel.menu.MenuService',
            function ($scope, $state, $stateParams, $rootScope, $log, ConfigService, FeedbackService, FormStateService, MenuService) {

                function onSuccess() {
                }

                function setErrorFeedback(errorResponse) {
                    $scope.$parent.feedback = FeedbackService.getFeedback(errorResponse);
                }

                function editItem(itemId) {
                    $scope.$parent.feedback = '';

                    $state.go('menu-item.edit', {
                        menuItemId: itemId
                    });
                }

                $scope.callbacks = {
                    itemClicked: function (itemScope) {
                        var clickedItemId = itemScope.id;

                        if (FormStateService.isDirty()) {
                            if (FormStateService.isValid()) {
                                MenuService.saveMenuItem($scope.$parent.selectedMenuItem).then(function() {
                                        editItem(clickedItemId);
                                    },
                                    function (error) {
                                        setErrorFeedback(error);
                                        FormStateService.setValid(false);
                                    }
                                );
                            }
                        } else {
                            editItem(clickedItemId);
                        }
                    },
                    itemMoved: function (sourceScope, modelData, sourceIndex, destScope, destIndex) {
                        // created an issue for the Tree component, to add a disabled state
                        // link: https://github.com/JimLiu/angular-ui-tree/issues/63
                        if (!FormStateService.isValid()) {
                            moveItemModel(sourceScope, sourceIndex, destScope, destIndex);
                        } else {
                            var parentData = destScope.parentItemScope(),
                                destId = (!parentData) ? ConfigService.menuId : parentData.itemData().id;
                            MenuService.moveMenuItem(modelData.id, destId, destIndex).then(onSuccess, function (errorResponse) {
                                setErrorFeedback(errorResponse);
                                moveItemModel(sourceScope, sourceIndex, destScope, destIndex);
                            });
                        }

                        // prevent move action when the edit menu item form is invalid
                        MenuService.saveMenuItem($scope.$parent.selectedMenuItem);
                    },
                    orderChanged: function (scope, modelData, sourceIndex, destIndex) {
                        if (!FormStateService.isValid()) {
                            // revert tree, move the item back at it's original place in the DOM
                            moveItemModel(scope, sourceIndex, scope, destIndex);
                        } else {
                            var parentData = scope.parentItemScope(),
                                destId = (!parentData) ? ConfigService.menuId : parentData.itemData().id;
                            MenuService.moveMenuItem(modelData.id, destId, destIndex).then(onSuccess, setErrorFeedback);
                        }
                    }
                };
            }
        ]);
}());
