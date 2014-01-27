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

    angular.module('hippo.channelManager.menuManagement')

        .controller('hippo.channelManager.menuManagement.EditMenuItemCtrl', [
            '$scope',
            '$routeParams',
            '$location',
            'hippo.channelManager.menuManagement.MenuService',
            'hippo.channelManager.menuManagement.ConfigService',
            function ($scope, $routeParams, $location, MenuService, ConfigService) {
                // scope values
                $scope.selectedMenuItemId = $routeParams.menuItemId;
                $scope.menuTree = [{name: 'Placeholder'}];
                $scope.menuItemDestinations = [{}];

                // methods
                $scope.navigateTo = function (branch) {
                    if ($scope.selectedMenuItemId !== branch.id) {
                        $location.path('/' + branch.id + '/edit');
                    }
                };

                $scope.createNewPage = function () {
                    $location.path('/' + $scope.selectedMenuItemId + '/add-page');
                };

                // fetch initial data
                MenuService.getMenu(ConfigService.menuId).then(function (response) {
                    $scope.menuTree = response.children;
                });

                // make sure there is a destination property
                $scope.$watch('selectedMenuItem', function (item) {
                    if (item && !item.destination) {
                        if (item.externalLink) {
                            item.destination = 2;
                        } else if (item.siteMapItemPath) {
                            item.destination = 1;
                        } else {
                            item.destination = 3;
                        }
                    }
                });
        }]);
})();