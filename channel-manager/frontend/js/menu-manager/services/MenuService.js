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

        .service('hippo.channelManager.menuManager.MenuService', [
            'hippo.channelManager.menuManager.ConfigService',
            '$http',
            '$q',
            function (ConfigService, $http, $q) {
                var menuService = {},
                    menuData = {
                        children: null
                    },
                    menuDataLoading = false,
                    menuLoaded = null;

                function menuServiceUrl(suffix) {
                    var url = ConfigService.apiUrlPrefix + '/' + ConfigService.menuId;
                    if (angular.isString(suffix)) {
                        url += './' + suffix;
                    }
                    return url;
                }

                function loadMenu() {
                    if (!menuDataLoading) {
                        menuDataLoading = true;
                        menuLoaded = $q.defer();
                        return $http.get(menuServiceUrl())
                            .success(function (response) {
                                menuData.children = response.data.children;
                                menuLoaded.resolve(menuData);
                            })
                            .error(function (error) {
                                menuLoaded.reject(error);
                            })
                            .then(function () {
                                menuDataLoading = false;
                            });
                    }
                }

                function loadMenuOnce() {
                    if (menuData.children === null) {
                        loadMenu();
                    }
                }

                function findMenuItem(items, id) {
                    var found = _.findWhere(items, { id: id });
                    if (found === undefined && angular.isArray(items)) {
                        for (var i = 0, length = items.length; i < length && !found; i++) {
                            found = findMenuItem(items[i].children, id);
                        }
                    }
                    return found;
                }

                function getMenuItem(id) {
                    return findMenuItem(menuData.children, id);
                }

                function whenMenuLoaded(getResolved) {
                    var deferred = $q.defer();
                    menuService.getMenu().then(
                        function() {
                            var resolved = angular.isFunction(getResolved) ? getResolved() : undefined;
                            deferred.resolve(resolved);
                        },
                        function(error) {
                            deferred.reject(error);
                        }
                    );
                    return deferred.promise;
                }

                menuService.getMenu = function () {
                    loadMenuOnce();
                    return menuLoaded.promise;
                };

                menuService.getFirstMenuItemId = function () {
                    return whenMenuLoaded(function () {
                        return menuData.children[0].id;
                    });
                };

                menuService.getMenuItem = function (menuItemId) {
                    return whenMenuLoaded(function () {
                        return getMenuItem(menuItemId);
                    });
                };

                menuService.saveMenuItem = function (menuItem) {
                    return $http.post(menuServiceUrl(), menuItem)
                        .error(function (error) {
                            // TODO show error in UI
                            console.error("An error occurred while saving the menu item with id '" + menuItem.id + "': " + error);
                        });
                };

                menuService.createMenuItem = function (parentId, menuItem) {
                    var deferred = $q.defer();
                    $http.post(menuServiceUrl('create/' + parentId), menuItem)
                        .success(function(response) {
                            deferred.resolve(response.data);
                        })
                        .error(function () {
                            // TODO show error in UI
                            console.error("An error occured while creating a menu item");
                            deferred.reject();
                        });
                    return deferred.promise;
                };

                menuService.deleteMenuItem = function (menuItemId) {
                    var deferred = $q.defer();

                    $http.post(menuServiceUrl('delete/' + menuItemId))
                        .success(function() {
                            loadMenu();
                            deferred.resolve();
                        })
                        .error(function () {
                            deferred.reject();
                        });

                    return deferred.promise;
                };

                menuService.moveMenuItem = function (menuItemId, newParentId, newPosition) {
                    newParentId = (newParentId === '#') ? ConfigService.menuId : newParentId;
                    var url = menuServiceUrl('move/' + menuItemId + '/' + newParentId + '/' + newPosition );

                    $http.post(url)
                        .error(function (error) {
                            // TODO show error in UI
                            console.error("An error occurred while moving the menu item with id '" + menuItemId + "': ", error);
                        });
                };

                menuService.loadMenu = function () {
                    return loadMenu();
                };

                return menuService;
            }
        ]);
}());
