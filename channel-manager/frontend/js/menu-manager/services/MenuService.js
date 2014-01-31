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

        .service('hippo.channelManager.menuManagement.MenuService', ['hippo.channelManager.menuManagement.ConfigService', '$q', '$http', function (ConfigService, $q, $http) {
            var menuService = {};

            // fetch menu tree
            menuService.getMenu = function(menuId) {
                var deferred = $q.defer();

                $http.get(ConfigService.apiUrlPrefix + '/' + menuId).success(function (response) {
                    deferred.resolve(response.data);
                }).error(function (error) {
                    deferred.reject('An error occured while fetching the menu tree with id `' + menuId + '`: ' + error);
                });

                return deferred.promise;
            };

            // create new child item
            // TODO: finish this implementation
            menuService.createItem = function (parentId, menuItem) {
                var deferred = $q.defer();

                $http.post(ConfigService.apiUrlPrefix + '/' + parentId, menuItem).success(function (response) {
                    deferred.resolve(response.data);
                }).error(function() {
                    deferred.reject('An error occured while fetching all personas');
                });

                return deferred.promise;
            };

            return menuService;
        }]);
}());