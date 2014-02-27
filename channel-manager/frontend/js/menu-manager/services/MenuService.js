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
            '$log',
            function (ConfigService, $http, $q) {
                var menuService = {},
                    menuData = {
                        items: null
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
                                menuData.items = response.data.items;
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
                    if (menuData.items === null) {
                        loadMenu();
                    }
                }

                function findMenuItem(items, id) {
                    var found = _.findWhere(items, { id: id });
                    if (found === undefined && angular.isArray(items)) {
                        for (var i = 0, length = items.length; i < length && !found; i++) {
                            found = findMenuItem(items[i].items, id);
                        }
                    }
                    return found;
                }

                function getMenuItem(id) {
                    return findMenuItem(menuData.items, id);
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

                function findItemAndItsParent(itemId, parent) {
                    var items = parent.items;
                    var item = _.findWhere (items, {id: itemId});
                    if (item) {
                        return {item: item, parent: parent};
                    } else if (items) {
                        var found;
                        for (var i = 0, length = items.length; i < length && !found; i++) {
                            found = findItemAndItsParent(itemId, items[i]);
                        }
                        return found;
                    } else {
                        return null;
                    }
                }

                function getSelectedItemIdBeforeDeletion(toBeDeletedItemId) {
                    var itemWithParent = findItemAndItsParent(toBeDeletedItemId, menuData);
                    var parent = itemWithParent.parent;
                    var items = parent.items;
                    if (items.length == 1) {
                        // item to delete has no siblings, so parent will be selected
                        return parent.id;
                    }
                    var itemIndex = _.indexOf(items, itemWithParent.item);
                    if (itemIndex === 0) {
                        // Item to delete is first child, so select next child
                        return items[itemIndex + 1].id;
                    } else {
                        // Item to delete is not first child, so select previous child
                        return items[itemIndex - 1].id;
                    }
                }

                menuService.getMenu = function () {
                    loadMenuOnce();
                    return menuLoaded.promise;
                };

                menuService.getFirstMenuItemId = function () {
                    return whenMenuLoaded(function () {
                        return menuData.items[0].id;
                    });
                };

                menuService.getMenuItem = function (menuItemId) {
                    return whenMenuLoaded(function () {
                        return getMenuItem(menuItemId);
                    });
                };

                menuService.saveMenuItem = function (menuItem) {
                    var deferred = $q.defer();
                    $http.post(menuServiceUrl(), menuItem)
                        .success(function() {
                            deferred.resolve();
                        })
                        .error(function (errorResponse) {
                                deferred.reject(errorResponse);
                            });
                    return deferred.promise;
                };

                menuService.createMenuItem = function (parentId, menuItem) {
                    var deferred = $q.defer();
                    $http.post(menuServiceUrl('create/' + parentId), menuItem)
                        .success(function(response) {
                            deferred.resolve(response.data);
                        })
                        .error(function (errorResponse) {
                                deferred.reject(errorResponse);
                            });
                    return deferred.promise;
                };

                menuService.deleteMenuItem = function (menuItemId) {
                    var deferred = $q.defer();
                    $http.post(menuServiceUrl('delete/' + menuItemId))
                        .success(function() {
                            var selectedItemId = getSelectedItemIdBeforeDeletion(menuItemId);
                            loadMenu();
                            deferred.resolve(selectedItemId);
                        })
                        .error(function (errorResponse) {
                                deferred.reject(errorResponse);
                            });
                    return deferred.promise;
                };

                menuService.moveMenuItem = function (menuItemId, newParentId, newPosition) {
                    newParentId = (newParentId === '#') ? ConfigService.menuId : newParentId;
                    var url = menuServiceUrl('move/' + menuItemId + '/' + newParentId + '/' + newPosition );

                    var deferred = $q.defer();
                    $http.post(url)
                        .success(function () {
                            deferred.resolve();
                        })
                        .error(function (errorResponse) {
                            deferred.reject(errorResponse);
                        });
                    return deferred.promise;
                };

                menuService.loadMenu = function () {
                    return loadMenu();
                };

                return menuService;
            }
        ]);
}());
