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
                items: []
            };

            if (menuItem.children && menuItem.children.length > 0) {
                node.items = createTree(menuItem.children);
            }

            nodes.push(node);
        });

        return nodes;
    }

    angular.module('hippo.channelManager.menuManager')

        .controller('hippo.channelManager.menuManager.TreeCtrl', [
            '$scope',
            '$state',
            'hippo.channelManager.menuManager.ConfigService',
            'hippo.channelManager.menuManager.MenuService',
            function ($scope, $state, ConfigService, MenuService) {
                $scope.list = [];

                MenuService.getMenu().then(function(menuData) {
                    $scope.list = createTree(menuData.children);
                });

                $scope.navigateTo = function (itemId) {
                    $state.go('menu-item.edit', {menuItemId: itemId});
                };

                $scope.moveNode = function (node) {
                    MenuService.moveMenuItem(node.id, node.newParentId, node.position);
                };
            }
        ]);
}());