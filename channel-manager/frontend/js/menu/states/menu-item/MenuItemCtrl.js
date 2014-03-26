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

        .controller('hippo.channel.menu.MenuItemCtrl', [
            '$scope',
            '$rootScope',
            '$state',
            'hippo.channel.menu.MenuService',
            function ($scope, $rootScope, $state, MenuService) {
                $scope.list = [];
                $scope.selectedMenuItem = {};
                $scope.feedback = {};

                // initial load of menu tree structure
                MenuService.getMenu().then(function (menuData) {
                    $scope.list = menuData.items;
                    $scope.selectedMenuItem = $scope.list.length > 0 ? $scope.list[0] : undefined;

                    $scope.$watch(function() {
                        return menuData.items;
                    }, function() {
                        $scope.list = menuData.items;

                        // merge pending changes into newly loaded tree
                        if ($scope.selectedMenuItem) {
                            MenuService.getMenuItem($scope.selectedMenuItem.id).then(function(item) {
                                if ($scope.selectedMenuItem != item) {
                                    delete $scope.selectedMenuItem.items;
                                    $scope.selectedMenuItem = angular.extend(item, $scope.selectedMenuItem);
                                }
                            });
                        }
                    }, false);
                });

                // if we redirect to a url without DOM-interaction, we need to set the selected menu item manually
                $rootScope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState, fromParams) {
                    if (toState.name == 'menu-item.edit' &&
                            (!$scope.selectedMenuItem || toParams.menuItemId != $scope.selectedMenuItem.id)) {
                        MenuService.getMenuItem(toParams.menuItemId).then(function (item) {
                            $scope.selectedMenuItem = item;
                        });
                    }
                });

                $rootScope.$on('before-close', function(event) {
                    if ($state.current.name == 'menu-item.add') {
                        event.preventDefault();
                    }
                });
            }
        ]);
}());
