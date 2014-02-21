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

        .controller('hippo.channelManager.menuManager.MenuItemCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            '$log',
            'hippo.channelManager.menuManager.MenuService',
            function ($scope, $state, $stateParams, $log, MenuService) {
                $scope.confirmation = {
                    isVisible: false
                };

                $scope.remove = function () {
                    $log.info('Delete menu item');

                    $log.info($stateParams);
                    $log.info(window.location);

                    // TODO: get the actual menuItemId, stateParams does not contain menuItemId
                    var menuItemId = $stateParams.menuItemId;

                    MenuService.deleteMenuItem(menuItemId).then(function () {
                        MenuService.getFirstMenuItemId().then(
                            function (firstMenuItemId) {
                                // TODO: be smarter about which item we select. For now we select the first one again
                                $state.go('menu-item.edit', {
                                    menuItemId: firstMenuItemId
                                });
                            },
                            function (error) {
                                // TODO show error in UI
                                $log.error(error);
                            }
                        );
                    });
                };

                // add menu item
                $scope.add = function () {
                    $log.info('Add menu item');
                };
            }
        ]);
}());