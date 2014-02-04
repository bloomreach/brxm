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
            '$routeParams',
            '$stateParams',
            '$state',
            'hippo.channelManager.menuManager.MenuService',
            'hippo.channelManager.menuManager.ConfigService',
            function ($scope, $routeParams, $stateParams, $state, MenuService, ConfigService) {
                console.log('edit menu item ctrl init');

                var destinationLookupTable = {
                    'SITEMAPITEM': 1,
                    'EXTERNAL': 2,
                    'NONE': 3};

                // fetch menu item
                MenuService.getMenuItem(ConfigService.menuId, $stateParams.menuItemId).then(function (response) {
                    setDestinationProperty(response);
                    $scope.selectedMenuItem = response;
                });

                function setDestinationProperty(item) {
                    if (item && !item.destination) {
                        item.destination = destinationLookupTable[item.linkType];
                    }
                }

                $scope.createNewPage = function () {
                    $state.go('menu-item.add-page', {menuItemId: $stateParams.menuItemId });
                };
            }
        ]);
}());
