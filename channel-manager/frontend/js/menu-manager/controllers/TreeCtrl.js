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

    function reformatData(src) {
        var result = [];
        _.each(src, function (item) {
            var newItem = item;
            newItem.text = item.name;

            if (item.children && item.children.length > 0) {
                newItem.children = reformatData(item.children);
            }

            result.push(newItem);
        });

        return result;
    }

    angular.module('hippo.channelManager.menuManagement')

        .controller('hippo.channelManager.menuManagement.TreeCtrl', [
            '$scope',
            'hippo.channelManager.menuManagement.ConfigService',
            'hippo.channelManager.menuManagement.MenuService',
            function ($scope, ConfigService, MenuService) {

                // fetch initial data
                $scope.menuTree = [{}];
                MenuService.getMenu(ConfigService.menuId).then(function (response) {
                    $scope.menuTree = reformatData(response.children);
                });

                // methods
                $scope.setSelectedItemId = function (/*itemId*/) {
                    // TODO: fetch the details for this menu item and set them as the selectedMenuItem
                    MenuService.getMenu(ConfigService.menuId).then(function () {
                        $scope.$parent.selectedMenuItem = {
                            id: 'abc-123',
                            name: 'Placeholder text - ' + Math.ceil((Math.random() * 10))
                        };
                    });

                    // set selected menu item so child-controllers can access it
                    /*
                     MenuService.getMenu(itemId).then(function (response) {
                     console.log(response);
                     $scope.$parent.selectedMenuItem = response;
                     });
                     */

                    /*
                    // redirect if the selected item is different from the current
                    if ($scope.$parent.selectedMenuItemId !== itemId) {
                        $location.path('/' + itemId + '/edit');
                    }
                    */
                };

            }]);
}());