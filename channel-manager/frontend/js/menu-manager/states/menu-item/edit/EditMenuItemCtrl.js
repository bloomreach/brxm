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

        .controller('hippo.channelManager.menuManager.EditMenuItemCtrl', [
            '$scope',
            '$stateParams',
            '$state',
            'hippo.channelManager.menuManager.MenuService',
            'hippo.channelManager.menuManager.FocusService',
            function ($scope, $stateParams, $state, MenuService, FocusService) {
                var savedMenuItem;

                function shouldSaveMenuItem(menuItem) {
                    if (!angular.isDefined(menuItem)) {
                        return false;
                    }
                    return menuItem.name !== savedMenuItem.name
                        || menuItem.link !== savedMenuItem.link
                        || menuItem.linkType !== savedMenuItem.linkType;
                }

                MenuService.getMenuItem($stateParams.menuItemId).then(function (menuItem) {
                    $scope.selectedMenuItem = menuItem;
                    savedMenuItem = angular.copy($scope.selectedMenuItem);
                });

                $scope.saveMenuItem = function () {
                    if (shouldSaveMenuItem($scope.selectedMenuItem)) {
                        savedMenuItem = angular.copy($scope.selectedMenuItem);
                        MenuService.saveMenuItem($scope.selectedMenuItem);
                    }
                };

                $scope.focus = FocusService.focusElementWithId;

                $scope.createNewPage = function () {
                    $state.go('menu-item.add-page', { menuItemId: $stateParams.menuItemId });
                };
            }
        ]);
}());
