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
                    resolve: { factory: dispatchToDesiredPage }
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
                        return 'tool/' + $stateParams.id + '/' + $stateParams.id + '.html';
                    }
                })
                .state('home', {
                    controller: 'homeCtrl',
                    url: '/home',
                    templateUrl: 'pages/home.html'
                })
                .state('library', {
                    url: '/library',
                    templateUrl: 'pages/library.html',
                    controller: 'pluginCtrl'
                })
                .state('build', {
                    url: '/build',
                    templateUrl: 'pages/build.html',
                    controller: 'pluginCtrl'
                })
                .state('installed-features', {
                    url: '/installed-features',
                    templateUrl: 'pages/installed-features.html',
                    controller: 'pluginCtrl'
                })
                .state('plugin', {
                    url: '/plugins/:id',
                    templateUrl: function ($stateParams) {
                        return 'feature/' + $stateParams.id + '/' + $stateParams.id + '.html';
                    }
                }
            );
        });

    var dispatchToDesiredPage = function ($q, $rootScope, $location, $http, $log) {
        // Determine the correct location path to start.
        // we do this using the Deferred API, such that the path gets resolved before the controller is initialized.
        var deferred = $q.defer();

        $location.path("/library"); // Start in the library by default.

        $http.get($rootScope.REST.packageStatus)
            .success(function (response) {
                if (response.status) {
                    // project setup has happened.
                    // Do a ping to decide whether to go to library or installed features
                    $http.get($rootScope.REST.ping)
                        .success(function (data) {
                            if (data.installedFeatures > 0) {
                                $location.path("/installed-features");
                            }
                            deferred.resolve(true);
                        })
                        .error(function () {
                            deferred.reject();
                        });
                } else {
                    // project needs setup.
                    $location.path("/introduction");
                    deferred.resolve(true);
                }
            })
            .error(function () {
                deferred.reject();
            });

        return deferred.promise;
    }
})();

