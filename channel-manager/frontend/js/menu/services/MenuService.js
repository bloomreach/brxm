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

        .service('hippo.channel.menu.MenuService', [
            'hippo.channel.ConfigService',
            '$http',
            '$q',
            '$log',
            function (ConfigService, $http, $q) {
                var menuData = {
                        items: null
                    },
                    menuLoader = null,
                    writeQueue = [];

                function menuServiceUrl(suffix) {
                    var url = ConfigService.apiUrlPrefix + '/' + ConfigService.menuId;
                    if (angular.isString(suffix)) {
                        url += './' + suffix;
                    }
                    return url;
                }

                function loadMenu() {
                    if (menuLoader === null) {
                        menuLoader = $q.defer();
                        $http.get(menuServiceUrl())
                            .success(function (response) {
                                menuData.items = response.data.items;
                                menuData.id = response.data.id;
                                menuLoader.resolve(menuData);
                            })
                            .error(function (error) {
                                menuLoader.reject(error);
                            });
                    }
                    return menuLoader.promise;
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

                function findPathToMenuItem(parent, id) {
                    var found;
                    _.every(parent.items, function (item) {
                        if (item.id == id) {
                            found = [item];
                        } else if (item.items) {
                            found = findPathToMenuItem(item, id);
                        }
                        return found === undefined;
                    });
                    if (found) {
                        found.unshift(parent);
                    }
                    return found;
                }

                function getMenuItem(id) {
                    return findMenuItem(menuData.items, id);
                }

                function whenMenuLoaded(getResolved) {
                    var deferred = $q.defer();
                    loadMenu().then(
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

                function post(url, body) {
                    var deferred = $q.defer();

                    writeQueue.push({
                        url: url,
                        body: body,
                        deferred : deferred
                    });

                    function onSuccess(response) {
                        var head = writeQueue.shift();
                        head.deferred.resolve(response);

                        if (writeQueue.length === 0) {
                            menuLoader = null;
                        } else {
                            scheduleNext();
                        }
                    }

                    function onError(response) {
                        var head = writeQueue.shift();
                        head.deferred.reject(response);

                        while (writeQueue.length > 0) {
                            var entry = writeQueue.shift();
                            entry.deferred.reject();
                        }
                    }

                    function scheduleNext() {
                        var head = writeQueue[0];
                        $http.post(head.url, head.body).success(onSuccess).error(onError);
                    }

                    if (writeQueue.length === 1) {
                        scheduleNext();
                    }

                    return deferred.promise;
                }

                return {

                    FIRST : 'first',
                    AFTER : 'after',

                    getMenu : function () {
                        return loadMenu();
                    },

                    getPathToMenuItem : function(menuItemId) {
                        return whenMenuLoaded(function () {
                            return findPathToMenuItem(menuData, menuItemId);
                        });
                    },

                    getMenuItem : function (menuItemId) {
                        return whenMenuLoaded(function () {
                            return getMenuItem(menuItemId);
                        });
                    },

                    saveMenuItem : function (menuItem) {
                        var deferred = $q.defer();
                        post(menuServiceUrl(), menuItem).then(function() {
                                    deferred.resolve();
                                }, function (errorResponse) {
                                    deferred.reject(errorResponse);
                                });
                        return deferred.promise;
                    },

                    /**
                     * Create a new menu item.

                     * @param parentItemId When specified, the item will be created under the parent.
                     *                     Otherwise, the item will be created as a root item.
                     * @param menuItem The item to be created
                     * @param options item positioning details;
                     *      { position: <position> , siblingId: <sibling> }
                     *      with position either MenuService.FIRST or MenuService.AFTER.  The siblingId
                     *      is taken into account when the position is AFTER.
                     * @returns {promise|Promise.promise|Q.promise}
                     */
                    createMenuItem : function (parentItemId, menuItem, options) {
                        var deferred = $q.defer(), parentId = parentItemId;
                        if (parentId === undefined) {
                            parentId = ConfigService.menuId;
                        }
                        post(menuServiceUrl('create/' + parentId
                                                + (options ? '?position=' + options.position
                                                + (options.siblingId ? ('&sibling=' + options.siblingId) : '') : '')), menuItem)
                            .then(function(response) {
                                        menuItem.id = response.data;
                                        loadMenu().then(function() {
                                            deferred.resolve(response.data);
                                        }, function () {
                                            deferred.resolve(response.data);
                                        });
                                    }, function (errorResponse) {
                                        deferred.reject(errorResponse);
                                    });
                        return deferred.promise;
                    },

                    deleteMenuItem : function (menuItemId) {
                        var deferred = $q.defer();
                        post(menuServiceUrl('delete/' + menuItemId))
                            .then(function() {
                                deferred.resolve();
                            }, function (errorResponse) {
                                    deferred.reject(errorResponse);
                                });
                        return deferred.promise;
                    },

                    moveMenuItem : function (menuItemId, newParentId, newPosition) {
                        newParentId = (newParentId === '#') ? ConfigService.menuId : newParentId;
                        var url = menuServiceUrl('move/' + menuItemId + '/' + newParentId + '/' + newPosition );

                        var deferred = $q.defer();
                        post(url, {})
                            .then(function (data) {
                                deferred.resolve(data);
                            }, function (errorResponse) {
                                deferred.reject(errorResponse);
                            });
                        return deferred.promise;
                    },

                    processAllChanges: function() {
                        var deferred = $q.defer();

                        if (writeQueue.length === 0) {
                            deferred.resolve();
                        } else {
                            _.last(writeQueue).deferred.promise.then(function() {
                                deferred.resolve();
                            });
                        }

                        return deferred.promise;
                    }

                };
            }

        ]);
}());
