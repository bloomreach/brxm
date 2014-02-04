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

    angular.module('hippo.channelManager.menuManager')

        .controller('hippo.channelManager.menuManager.TreeCtrl', [
            '$scope',
            '$state',
            'hippo.channelManager.menuManager.ConfigService',
            'hippo.channelManager.menuManager.MenuService',
            function ($scope, $state, ConfigService, MenuService) {

                console.log('tree ctrl init');

                // fetch initial data
                $scope.menuTree = [{}];
                MenuService.getMenu(ConfigService.menuId).then(function (response) {
                    $scope.menuTree = reformatData(response.children);
                });

                $scope.setSelected = function (itemId) {
                    $state.go('menu-item.edit', {menuItemId: itemId});
                };

            }
        ]);
}());