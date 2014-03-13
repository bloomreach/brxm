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

//############################################
// ROUTES
//############################################
// configure our routes
(function () {
    "use strict";

    angular.module('hippo.essentials')
            .config(function ($stateProvider, $routeProvider) {
                $stateProvider
                        .state('index', {
                            controller: 'homeCtrl',
                            url: '',
                            templateUrl: '../plugin-dashboard/src/main/webapp/pages/home.html',
                            resolve: { factory: checkPackInstalled}
                        })
                        .state('tools', {
                            url: '/tools',
                            templateUrl: '../plugin-dashboard/src/main/webapp/pages/tools.html',
                            controller: 'pluginCtrl'
                        })
                        .state('powerpacks', {
                            url: '/powerpacks',
                            templateUrl: '../plugin-dashboard/src/main/webapp/powerpacks/index.html',
                            controller: 'powerpacksCtrl'
                        })
                        .state('powerpacks-id', {
                            url: '/powerpacks/:id',
                            templateUrl: function ($stateParams) {
                                return '../plugin-dashboard/src/main/webapp/powerpacks/' + $stateParams.id + '/' + $stateParams.id + '.html';
                            }
                        })
                        .state('tools-id', {
                            url: '/tools/:id',
                            templateUrl: function ($stateParams) {
                                return '../plugin-dashboard/src/main/webapp/tools/' + $stateParams.id + '/' + $stateParams.id + '.html';
                            }
                        })
                        .state('home', {
                            controller: 'homeCtrl',
                            url: '/home',
                            templateUrl: '../plugin-dashboard/src/main/webapp/pages/home.html'

                        })
                        .state('plugins', {
                            url: '../plugin-dashboard/src/main/webapp/plugins/',
                            templateUrl: '../plugin-dashboard/src/main/webapp/pages/plugins.html',
                            controller: 'pluginCtrl',
                            views: {
                                "submenu": {
                                    templateUrl: '../plugin-dashboard/src/main/webapp/pages/plugins-menu.html',
                                    controller: 'pluginCtrl'
                                }, "plugintabs": {
                                    templateUrl: 'pages/plugins-installed-tabs.html',
                                    controller: 'pluginCtrl'
                                }

                            }

                        })
                        .state('find-plugins', {
                            url: '/find-plugins',
                            templateUrl: '../plugin-dashboard/src/main/webapp/pages/find-plugins.html',
                            controller: 'pluginCtrl',
                            views: {
                                "submenu": {
                                    templateUrl: '../plugin-dashboard/src/main/webapp/pages/plugins-menu-find.html',
                                    controller: 'pluginCtrl'
                                }, "plugintabs": {
                                    templateUrl: '../plugin-dashboard/src/main/webapp/pages/plugins-new-tabs.html',
                                    controller: 'pluginCtrl'
                                }, "plugininstance": {
                                    controller: 'pluginCtrl',
                                    templateUrl: '../plugin-dashboard/src/main/webapp/pages/find-plugins.html'
                                }
                            }
                        }
                )
                        .state('plugin', {
                            url: '/plugins/:id',
                            views: {
                                "submenu": {
                                    templateUrl: '../plugin-dashboard/src/main/webapp/pages/plugins-menu-find.html',
                                    controller: 'pluginCtrl'
                                }, "plugintabs": {
                                    templateUrl: '../plugin-dashboard/src/main/webapp/pages/plugins-installed-tabs.html',
                                    controller: 'pluginCtrl'
                                }, "plugininstance": {
                                    url: '../plugin-dashboard/src/main/webapp/plugins/:id',
                                    templateUrl: function ($stateParams) {
                                        return '../plugin-dashboard/src/main/webapp/plugins/' + $stateParams.id + '/' + $stateParams.id + '.html';
                                    }
                                }
                            }
                        }
                );
            });
    var checkPackInstalled = function ($q, $rootScope, $location, $http, $log) {
        $rootScope.checkDone = true;
        if ($rootScope.packsInstalled) {
            $log.info("powerpack is installed");
            return true;
        } else {
            var deferred = $q.defer();
            $http.get($rootScope.REST.powerpacksStatus)
                    .success(function (response) {
                        $rootScope.packsInstalled = response.status;
                        deferred.resolve(true);
                        if (!$rootScope.packsInstalled) {
                            $location.path("/powerpacks");
                        }

                    })
                    .error(function () {
                        deferred.reject();
                        $rootScope.packsInstalled = false;
                        $location.path("/powerpacks");
                    });
            return deferred.promise;
        }
    }

})();

