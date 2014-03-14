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

        .controller('hippo.channel.menu.EditMenuItemCtrl', [
            '$scope',
            '$stateParams',
            '$state',
            '$log',
            '$window',
            'hippo.channel.FeedbackService',
            'hippo.channel.menu.MenuService',
            'hippo.channel.FormValidationService',
            '_hippo.channel.IFrameService',
            function ($scope, $stateParams, $state, $log, $window, FeedbackService, MenuService, FormValidationService, IFrameService) {
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

                $scope.showPage = function(link) {
                    var iframePanel = IFrameService.getContainer();
                    iframePanel.iframeToHost.publish('browseTo', (link.charAt(0) == '/' ? '' : '/') + link);
                };

                $scope.dismissFeedback = function () {
                    $scope.feedback.message = '';
                    $scope.fieldFeedbackMessage = {};
                };

                savedMenuItem = angular.copy($scope.selectedMenuItem);

                function shouldSaveSelectedMenuItemProperty() {
                    $scope.dismissFeedback();
                    if (!angular.isDefined($scope.selectedMenuItem)) {
                        return false;
                    }

                    return true;
                }

                function saveSelectedMenuItemProperty(propertyName) {
                    savedMenuItem = angular.copy($scope.selectedMenuItem);

                    // child properties haven't changed, so don't send them
                    delete savedMenuItem.items;

                    $scope.isSaving[propertyName] = true;

                    MenuService.saveMenuItem(savedMenuItem).then(function () {
                                $scope.isSaving[propertyName] = false;
                                $scope.isSaved[propertyName] = true;
                                FormValidationService.setValidity(true);
                            },
                            function (errorResponse) {
                                $scope.fieldFeedbackMessage[propertyName] = FeedbackService.getFeedback(errorResponse).message;
                                $scope.isSaving[propertyName] = false;
                                $scope.isSaved[propertyName] = false;
                                FormValidationService.setValidity(false);
                            }
                    );
                }

                function remove() {
                    // HTTP-request to delete the menu item
                    MenuService.deleteMenuItem($scope.selectedMenuItem.id).then(function (selectedMenuItemId) {
                        $state.go('menu-item.edit', {
                            menuItemId: selectedMenuItemId
                        });
                    }, function (errorResponse) {
                        $scope.feedback = FeedbackService.getFeedback(errorResponse);
                    });
                }

            }
        ]);
}());
