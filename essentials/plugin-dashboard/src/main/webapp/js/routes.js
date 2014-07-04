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
                    templateUrl: 'pages/home.html',
                    resolve: { factory: checkPackInstalled}
                })
                .state('tools', {
                    url: '/tools',
                    templateUrl: 'pages/tools.html',
                    controller: 'pluginCtrl'
                })
                .state('introduction', {
                    url: '/introduction',
                    templateUrl: 'pages/introduction.html',
                    controller: 'introductionCtrl'
                })
                .state('tools-id', {
                    url: '/tools/:id',
                    templateUrl: function ($stateParams) {
                        return 'tools/' + $stateParams.id + '/' + $stateParams.id + '.html';
                    }
                })
                .state('home', {
                    controller: 'homeCtrl',
                    url: '/home',
                    templateUrl: 'pages/home.html'

                })
                .state('plugins', {
                    url: '/plugins',
                    templateUrl: 'pages/plugins.html',
                    controller: 'pluginCtrl'
                })
                .state('find-plugins', {
                    url: '/find-plugins',
                    templateUrl: 'pages/find-plugins.html',
                    controller: 'pluginCtrl',
                    views: {
                        "submenu": {
                            templateUrl: 'pages/plugins-menu-find.html',
                            controller: 'pluginCtrl'
                        }, "plugintabs": {
                            templateUrl: 'pages/plugins-new-tabs.html',
                            controller: 'pluginCtrl'
                        }, "plugininstance": {
                            controller: 'pluginCtrl',
                            templateUrl: 'pages/find-plugins.html'
                        }
                    }
                }
            )
                .state('plugin', {
                    url: '/plugins/:id',
                    views: {
                        "submenu": {
                            templateUrl: 'pages/plugins-menu-find.html',
                            controller: 'pluginCtrl'
                        }, "plugintabs": {
                            templateUrl: 'pages/plugins-installed-tabs.html',
                            controller: 'pluginCtrl'
                        }, "plugininstance": {
                            url: '/plugins/:id',
                            templateUrl: function ($stateParams) {
                                return 'plugins/' + $stateParams.id + '/' + $stateParams.id + '.html';
                            }
                        }
                    }
                }
            );
        });
    var checkPackInstalled = function ($q, $rootScope, $location, $http, $log) {
        $rootScope.checkDone = true;
        if ($rootScope.packsInstalled) {
            $log.info("package is installed");
            return true;
        } else {
            var deferred = $q.defer();
            $http.get($rootScope.REST.packageStatus)
                .success(function (response) {
                    $rootScope.packsInstalled = response.status;
                    deferred.resolve(true);
                    if (!$rootScope.packsInstalled) {
                        $location.path("/introduction");
                    }

                })
                .error(function () {
                    deferred.reject();
                    $rootScope.packsInstalled = false;
                    $location.path("/introduction");
                });
            return deferred.promise;
        }
    }

})();

