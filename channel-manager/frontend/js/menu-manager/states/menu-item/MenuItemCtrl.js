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

    function createTree(menuItems) {
        var nodes = [];

        _.each(menuItems, function (menuItem) {
            var node = {
                id: menuItem.id,
                title: menuItem.name,
                items: [],
                link: menuItem.link,
                linkType: menuItem.linkType
            };

            if (menuItem.children && menuItem.children.length > 0) {
                node.items = createTree(menuItem.children);
            }

            nodes.push(node);
        });

        return nodes;
    }

    function findParentByItemId(sourceItem, itemId) {
        var result;

        if (sourceItem.items) {
            sourceItem.items.forEach(function (item) {
                if (item.id === itemId) {
                    result = sourceItem;
                }
            });
        }

        if (!result && sourceItem.items) {
            sourceItem.items.forEach(function (item) {
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
            sourceItem.forEach(function (item) {
                if (item.id === itemId) {
                    result = item;
                }
            });
        }

        if (!result) {
            sourceItem.forEach(function (item) {
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

                // initial load of menu tree structure
                MenuService.getMenu().then(function (menuData) {
                    $scope.list = createTree(menuData.children);
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
            }
        ]);
}());