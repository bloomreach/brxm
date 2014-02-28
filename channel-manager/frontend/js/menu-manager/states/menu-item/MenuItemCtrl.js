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

    function findParentByItemId(sourceItem, itemId) {
        var result;

        if (sourceItem.items) {
            angular.forEach(sourceItem.items, function (item) {
                if (item.id === itemId) {
                    result = sourceItem;
                }
            });
        }

        if (!result && sourceItem.items) {
            angular.forEach(sourceItem.items, function (item) {
                var newRes = findParentByItemId(item, itemId);
                if (newRes) {
                    result = item;
                }
            });
        }

        return result;
    }

    function findScopeByItemId(sourceItem, itemId) {
        var result;

        if (sourceItem) {
            angular.forEach(sourceItem, function (item) {
                if (item.id === itemId) {
                    result = item;
                }
            });
        }

        if (!result) {
            angular.forEach(sourceItem, function (item) {
                var newRes = findScopeByItemId(item.items, itemId);

                if (newRes) {
                    result = newRes;
                }
            });
        }

        return result;
    }

    angular.module('hippo.channelManager.menuManager')

        .controller('hippo.channelManager.menuManager.MenuItemCtrl', [
            '$scope',
            '$rootScope',
            'hippo.channelManager.menuManager.MenuService',
            function ($scope, $rootScope, MenuService) {
                $scope.list = [];
                $scope.selectedMenuItem = {};
                $scope.feedback = {};

                // initial load of menu tree structure
                MenuService.getMenu().then(function (menuData) {
                    $scope.list = menuData.items;
                    $scope.selectedMenuItem = $scope.list[0];
                });

                // if we redirect to a url without DOM-interaction, we need to set the selected menu item manually
                $rootScope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState, fromParams) {
                    if (fromState.name == 'menu-item.edit' && fromState.name == toState.name) {
                        if (toParams.menuItemId != $scope.selectedMenuItem.id) {
                            $scope.selectedMenuItem = findScopeByItemId($scope.list, toParams.menuItemId);
                        }
                    }
                });

                $scope.findParent = function (itemId) {
                    return findParentByItemId({items: $scope.list}, itemId);
                };

                $scope.dismissFeedback = function () {
                    $scope.feedback = {};
                };
            }
        ]);
}());
