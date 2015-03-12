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

    function getLink(menuItem) {
        var link;

        if (menuItem.pathInfo) {
            link = menuItem.mountPath + '/' + menuItem.pathInfo;
        } else {
            link = menuItem.link;
        }

        return link;
    }

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.EditMenuItemCtrl', [
            '$rootScope',
            '$scope',
            '$state',
            '$stateParams',
            '$window',
            'hippo.channel.FeedbackService',
            'hippo.channel.menu.MenuService',
            'hippo.channel.FormStateService',
            'hippo.channel.Container',
            function ($rootScope, $scope, $state, $stateParams, $window, FeedbackService, MenuService, FormStateService, ContainerService) {
                var savedMenuItem;

                if (!$scope.$parent.selectedMenuItem || $stateParams.menuItemId !== $scope.$parent.selectedMenuItem.id) {
                    MenuService.getMenuItem($stateParams.menuItemId).then(function (item) {
                        $scope.$parent.selectedMenuItem = item;
                    });
                }

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

                $scope.internalLink = {
                    openPicker: function() {
                        $state.go('picker', {
                            menuItemId: $scope.selectedMenuItem.id
                        });
                    },
                    showPage: function() {
                        var link = getLink($scope.selectedMenuItem);
                        ContainerService.showPage(link);
                    }
                };

                $scope.externalLink = {
                    isVisible: false,
                    show: function() {
                        $scope.externalLink.isVisible = true;
                    },
                    execute: function() {
                        $scope.externalLink.isVisible = false;
                        $window.open($scope.selectedMenuItem.link);
                    },
                    cancel: function() {
                        $scope.externalLink.isVisible = false;
                    }
                };

                $scope.fieldFeedbackMessage = {};

                $scope.saveTitle = function (form) {
                    if($scope.selectedMenuItem.isNew) {
                        form.title.$dirty = true;
                        delete $scope.selectedMenuItem.isNew;
                    }
                    if(form.title.$dirty && form.title.$valid) {
                        $scope.saveSelectedMenuItem('title');
                    }
                };

                $scope.updateLinkDestination = function (form) {
                    var formItem;
                    if($scope.selectedMenuItem.linkType === 'NONE') {
                        $scope.linkToFocus = 'none';
                        $scope.saveSelectedMenuItem('linkType');
                    } else {
                        if ($scope.selectedMenuItem.linkType === 'SITEMAPITEM') {
                            formItem = form.sitemapItem;
                            $scope.linkToFocus = 'sitemapLink';
                        } else if ($scope.selectedMenuItem.linkType === 'EXTERNAL') {
                            formItem = form.url;
                            $scope.linkToFocus = 'externalLink';
                        }
                        if(formItem.$dirty && formItem.$valid) {
                            $scope.saveSelectedMenuItem('link');
                        }
                    }
                };

                $scope.saveSelectedMenuItem = function(propertyName) {
                    if (shouldSaveSelectedMenuItemProperty(propertyName)) {
                        saveSelectedMenuItemProperty(propertyName);
                    }
                };

                $scope.dismissFeedback = function () {
                    if($scope.feedback.message) {
                        $scope.feedback.message = '';
                    }
                    $scope.fieldFeedbackMessage = {};
                };

                savedMenuItem = angular.copy($scope.selectedMenuItem);

                $scope.$watch(function() {
                    return $stateParams.selectedDocumentPath;
                }, function() {
                    $scope.selectedMenuItem.link = $scope.selectedMenuItem.sitemapLink = $stateParams.selectedDocumentPath;
                    $scope.linkToFocus = 'sitemapLink';
                    $scope.saveSelectedMenuItem('link');
                });

                function shouldSaveSelectedMenuItemProperty() {
                    $scope.dismissFeedback();
                    return angular.isDefined($scope.selectedMenuItem);
                }

                function saveSelectedMenuItemProperty(propertyName) {
                    savedMenuItem = angular.copy($scope.selectedMenuItem);

                    // child properties haven't changed, so don't send them
                    delete savedMenuItem.items;

                    $scope.isSaving[propertyName] = true;

                    MenuService.saveMenuItem(savedMenuItem)
                        .then(function () {
                            $scope.isSaving[propertyName] = false;
                            $scope.isSaved[propertyName] = true;
                            FormStateService.setValid(true);
                        },
                        function (errorResponse) {
                            $scope.fieldFeedbackMessage[propertyName] = FeedbackService.getFeedback(errorResponse).message;
                            $scope.isSaving[propertyName] = false;
                            $scope.isSaved[propertyName] = false;
                            FormStateService.setValid(false);
                        });
                }

                function remove() {
                    MenuService.getPathToMenuItem($scope.selectedMenuItem.id).then(function(path) {
                        var nextState = (function() {
                            var item, parent, items;
                            if (!path || path.length < 2) {
                                return { state: 'none' };
                            }

                            item = path.pop();
                            parent = path.pop();
                            items = parent.items;
                            if (items.length == 1) {
                                // item to delete has no siblings, so parent will be selected
                                if (path.length > 0) {
                                    return {state: 'edit', id: parent.id};
                                } else {
                                    return {state: 'none'};
                                }
                            }
                            var itemIndex = items.indexOf(item);
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
