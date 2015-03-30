/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
        .config(function ($stateProvider) {
            $stateProvider
                .state('index', {
                    url: '',
                    resolve: {factory: initAndDispatch}
                })
                .state('introduction', {
                    url: '/introduction',
                    templateUrl: 'pages/introduction.html',
                    controller: 'introductionCtrl',
                    resolve: {factory: initAndDispatch}
                })
                .state('library', {
                    url: '/library',
                    templateUrl: 'pages/library.html',
                    controller: 'pluginCtrl',
                    resolve: {factory: initAndDispatch}
                })
                .state('installed-features', {
                    url: '/installed-features',
                    templateUrl: 'pages/installed-features.html',
                    controller: 'pluginCtrl',
                    resolve: {factory: initAndDispatch}
                })
                .state('features', {
                    url: '/features/:id',
                    templateUrl: function ($stateParams) {
                        return 'feature/' + $stateParams.id + '/' + $stateParams.id + '.html';
                    },
                    resolve: {factory: initAndDispatch}
                })
                .state('tools', {
                    url: '/tools',
                    templateUrl: 'pages/tools.html',
                    controller: 'pluginCtrl',
                    resolve: {factory: initAndDispatch}
                })
                .state('tools-id', {
                    url: '/tools/:id',
                    templateUrl: function ($stateParams) {
                        return 'tool/' + $stateParams.id + '/' + $stateParams.id + '.html';
                    },
                    resolve: {factory: initAndDispatch}
                })
                .state('build', {
                    url: '/build',
                    templateUrl: 'pages/build.html',
                    controller: 'pluginCtrl',
                    resolve: {factory: initAndDispatch}
                });
        });

    var setupCompleted = false;
    var initAndDispatch = function ($q, $rootScope, $location, $http, $log, $timeout, modalService, pluginTypeFilter) {
        var startPinger = function () {
            var PING_RUNNING_TIMER = 7000;
            var PING_DOWN_TIMER = 10000;
            //############################################
            // PINGER
            //############################################
            // keep reference to old modal:
            var pingModal = null;
            (function ping() {

                var modalOptions = {
                    headerText: 'Service Down',
                    bodyText: '<p>The setup application server appears to be down. If you are' +
                    ' rebuilding the project, please wait until it is up and running again.</p>' +
                    ' <p>Remember, to rebuild and restart the project, execute:</p>' +
                    ' <pre>mvn verify</pre>' +
                    ' <pre>mvn -Pcargo.run -Drepo.path=storage</pre>'
                };

                function openModal() {
                    if (pingModal === null) {
                        pingModal = modalService.showModal({}, modalOptions);
                        pingModal.then(function () {
                            // discard modal
                            pingModal = null;
                        });
                    }
                }

                $http.get($rootScope.REST.project + '/ping').success(function (data) {
                    if (data) {
                        if (data.initialized) {
                            $timeout(ping, PING_RUNNING_TIMER);
                            $rootScope.TOTAL_PLUGINS = data.totalPlugins;
                            $rootScope.TOTAL_TOOLS = data.totalTools;
                            $rootScope.INSTALLED_FEATURES = data.installedFeatures;
                            $rootScope.NEEDS_REBUILD = data.needsRebuild;
                            $rootScope.TOTAL_NEEDS_ATTENTION = pluginTypeFilter(data.rebuildPlugins, "feature").length + data.configurablePlugins;
                            $rootScope.REBUILD_PLUGINS = data.rebuildPlugins;

                        } else {
                            // app is back up, but needs to restart
                            window.location.href = $rootScope.applicationUrl;
                        }
                    }
                }).error(function () {
                    openModal();
                    $timeout(ping, PING_DOWN_TIMER);
                });

            })();
        };
        // Determine the correct location path to start.
        // we do this using the Deferred API, such that the path gets resolved before the controller is initialized.
        var deferred = $q.defer();
        var completeInitialization = function () {
            setupCompleted = true;
            startPinger();

            // Dispatch to appropriate initial location
            $http.get($rootScope.REST.project + '/status')
                .success(function (response) {
                    if ($location.url() === '') { // only dispatch if not on specific "page".
                        if (!response.projectInitialized) {
                            $location.path("/introduction");
                        } else if (response.pluginsInstalled > 0) {
                            $location.path("/installed-features");
                        } else {
                            $location.path("/library");
                        }
                    }
                    deferred.resolve(true);
                })
                .error(function () {
                    $location.path("/library");
                    deferred.resolve(true);
                });
        };
        if (!setupCompleted) {
            $http.post($rootScope.REST.plugins + '/autosetup').then(completeInitialization, completeInitialization);
            return deferred.promise;
        }
    };
})();

