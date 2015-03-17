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

        .controller('hippo.channel.menu.MenuItemCtrl', [
            '$scope',
            '$filter',
            '$rootScope',
            '$state',
            'hippo.channel.menu.MenuService',
            function ($scope, $filter, $rootScope, $state, MenuService) {
                var MenuItemCtrl = this;
                MenuItemCtrl.treeItems = [];
                MenuItemCtrl.selectedMenuItem = {};
                MenuItemCtrl.feedback = {};

                // initial load of menu tree structure
                MenuService.getMenu().then(function (menuData) {
                    MenuItemCtrl.treeItems = menuData.items;
                    MenuItemCtrl.selectedMenuItem = MenuItemCtrl.treeItems.length > 0 ? MenuItemCtrl.treeItems[0] : undefined;

                    $scope.$watch(function() {
                        return menuData.items;
                    }, function() {
                        console.log('MenuItemCtrl.treeItems1', MenuItemCtrl.treeItems);
                        MenuItemCtrl.treeItems = menuData.items;
                        console.log('MenuItemCtrl.treeItems2', MenuItemCtrl.treeItems);
                        $scope.$broadcast('menu-items-changed');

                        // merge pending changes into newly loaded tree
                        if (MenuItemCtrl.treeItems.length > 0 && MenuItemCtrl.selectedMenuItem) {
                            MenuService.getMenuItem(MenuItemCtrl.selectedMenuItem.id).then(function(item) {
                                if (MenuItemCtrl.selectedMenuItem != item) {
                                    delete MenuItemCtrl.selectedMenuItem.items;
                                    MenuItemCtrl.selectedMenuItem = angular.extend(item, MenuItemCtrl.selectedMenuItem);
                                }
                            });
                        }
                    }, false);

                    MenuItemCtrl.showTooltip = function() {
                        var translate = $filter('translate');
                        MenuItemCtrl.tooltip = translate('TOOLTIP_OPEN_LINK');
                    };

                    MenuItemCtrl.hideTooltip = function() {
                        MenuItemCtrl.tooltip = '';
                    };
                });
            }
        ]);
}());
