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

    function getRenderPath(menuItem) {
        var renderPath;

        if (menuItem.pathInfo) {
            renderPath = menuItem.mountPath + '/' + menuItem.pathInfo;
        } else {
            renderPath = menuItem.link;
        }

        return renderPath;
    }

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.EditMenuItemCtrl', [
            '$scope',
            '$stateParams',
            '$state',
            '$log',
            '$window',
            'hippo.channel.FeedbackService',
            'hippo.channel.menu.MenuService',
            'hippo.channel.FormStateService',
            'hippo.channel.Container',
            function ($scope, $stateParams, $state, $log, $window, FeedbackService, MenuService, FormStateService, ContainerService) {
                var savedMenuItem;

                $scope.isSaving = {
                    title: false,
                    linkType: false,
                    link: false
                };

                $scope.isSaved = {
                    title: true,
                    linkType: true,
                    link: true
                };

                $scope.remove = {
                    isVisible: false,
                    show: function() {
                        $scope.remove.isVisible = true;
                    },
                    execute: function() {
                        $scope.remove.isVisible = false;
                        remove();
                    },
                    cancel: function() {
                        $scope.remove.isVisible = false;
                    }
                };

                $scope.external = {
                    isVisible: false,
                    show: function() {
                        $scope.external.isVisible = true;
                    },
                    execute: function() {
                        $scope.external.isVisible = false;
                        $window.open($scope.selectedMenuItem.link);
                    },
                    cancel: function() {
                        $scope.external.isVisible = false;
                    }
                };

                $scope.fieldFeedbackMessage = {
                };

                $scope.saveSelectedMenuItem = function(propertyName) {
                    if (shouldSaveSelectedMenuItemProperty(propertyName)) {
                        saveSelectedMenuItemProperty(propertyName);
                    }
                };

                $scope.createNewPage = function () {
                    $state.go('menu-item.add-page', {
                        menuItemId: $stateParams.menuItemId
                    });
                };

                $scope.showPage = function() {
                    var renderPath = getRenderPath($scope.selectedMenuItem);
                    ContainerService.showPage(renderPath);
                };

                $scope.dismissFeedback = function () {
                    $scope.feedback.message = '';
                    $scope.fieldFeedbackMessage = {};
                };

                savedMenuItem = angular.copy($scope.selectedMenuItem);

                function shouldSaveSelectedMenuItemProperty() {
                    $scope.dismissFeedback();
                    return angular.isDefined($scope.selectedMenuItem);
                }

                function saveSelectedMenuItemProperty(propertyName) {
                    savedMenuItem = angular.copy($scope.selectedMenuItem);

                    // child properties haven't changed, so don't send them
                    delete savedMenuItem.items;

                    $scope.isSaving[propertyName] = true;

                    MenuService.saveMenuItem(savedMenuItem).then(function () {
                                $scope.isSaving[propertyName] = false;
                                $scope.isSaved[propertyName] = true;
                                FormStateService.setValid(true);
                            },
                            function (errorResponse) {
                                $scope.fieldFeedbackMessage[propertyName] = FeedbackService.getFeedback(errorResponse).message;
                                $scope.isSaving[propertyName] = false;
                                $scope.isSaved[propertyName] = false;
                                FormStateService.setValid(false);
                            }
                    );
                }

                function remove() {
                    MenuService.getPathToMenuItem($scope.selectedMenuItem.id).then(function(path) {
                        var nextState = (function() {
                            var item, parent, items, state;
                            if (!path || path.length < 2) {
                                return { state: 'add', id: undefined };
                            }

                            item = path.pop();
                            parent = path.pop();
                            items = parent.items;
                            if (items.length == 1) {
                                // item to delete has no siblings, so parent will be selected
                                state = path.length > 0 ? 'edit' : 'add';
                                return {state: state, id: parent.id};
                            }
                            var itemIndex = _.indexOf(items, item);
                            if (itemIndex === 0) {
                                // Item to delete is first child, so select next child
                                return {state: 'edit', id:items[itemIndex + 1].id};
                            } else {
                                // Item to delete is not first child, so select previous child
                                return {state: 'edit', id:items[itemIndex - 1].id};
                            }
                        }());

                        // HTTP-request to delete the menu item
                        MenuService.deleteMenuItem($scope.selectedMenuItem.id).then(function () {
                            $state.go('menu-item.' + nextState.state, {
                                menuItemId: nextState.id
                            });
                        }, function (errorResponse) {
                            $scope.feedback = FeedbackService.getFeedback(errorResponse);
                        });
                    });


                }

            }
        ]);
}());
