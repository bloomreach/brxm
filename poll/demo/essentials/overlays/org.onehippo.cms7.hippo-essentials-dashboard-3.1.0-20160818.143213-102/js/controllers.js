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

(function () {
    "use strict";
    angular.module('hippo.essentials')

        .controller('introductionCtrl', function ($scope, $location, $sce, $log, $rootScope, $http) {
            // just sets a hide screen boolean flag to true
            $scope.showAdvanced = false;
            $scope.addUrl = function () {
                $scope.projectSettings.pluginRepositories.push('');
            };

            $scope.removeUrl = function (url) {
                var idx = $scope.projectSettings.pluginRepositories.indexOf(url);
                if (idx > -1) {
                    $scope.projectSettings.pluginRepositories.splice(idx, 1);
                }
            };
            $scope.getStarted = function () {
                // mark setup as done...
                $scope.projectSettings.setupDone = true;
                $http.post($rootScope.REST.PROJECT.settings, $scope.projectSettings).success(function () {
                    $location.path('/library'); // Start in the Library
                });

            };
            $scope.setup = function () {
                $http.get($rootScope.REST.PROJECT.settings).success(function (data) {
                    $scope.projectSettings = data;
                });
            };
            $scope.toggleAdvanced = function($event) {
                $event.preventDefault();
                $scope.showAdvanced = !$scope.showAdvanced;
            };
            // initialize
            $scope.setup();
        })

        .controller('pluginCtrl', function ($scope, $location, $sce, $log, $rootScope, $http) {

            $scope.allPluginsInstalled = "No additional plugins could be found";
            $scope.plugins = [];
            $scope.selectedPlugin = null;
            $scope.tabs = [
                {name: "Installed Plugins", link: "/plugins"},
                {name: "Find", link: "/find-plugins"}
            ];
            $scope.isInstalledFeature = function (plugin) {
                return plugin.type === 'feature' &&
                        plugin.installState === 'installed';
            };
            $scope.isInstalledFeatureAndRequiresAttention = function (plugin) {
                return plugin.type === 'feature' &&
                        plugin.installState !== 'discovered' &&
                        plugin.installState !== 'installed';
            };
            $scope.isInstalledTool = function (plugin) {
                return plugin.type === 'tool'; // TODO: handle install state.
            };
            // Receive state updates from individual plugins
            $rootScope.$on('update-plugin-install-state', function(event, data) {
                angular.forEach($scope.plugins, function (plugin) {
                    if (plugin.id === data.pluginId) {
                        plugin.installState = data.state;
                    }
                });
            });

            //fetch plugin list
            $scope.init = function () {

                // Retrieve project settings
                $http.get($rootScope.REST.PROJECT.settings).success(function (data) {
                    // TODO: ugly. If this needs to be on the rootScope, it should be put there during initialization
                    // of the angular app.
                    $rootScope.projectSettings = data;
                });

                if ($rootScope.pluginsCache) {
                    processItems($rootScope.pluginsCache);
                } else {
                    $http.get($rootScope.REST.plugins).success(function (data) {
                        var items = data.items;
                        $rootScope.pluginsCache = items;
                        processItems(items);
                    });
                }

                function processItems(items) {
                    $scope.plugins = items;
                }
            };
            $scope.init();
        })

        /*
         //############################################
         // MENU CONTROLLER
         //############################################
         */
        .controller('mainMenuCtrl', ['$scope', '$location', '$rootScope', function ($scope, $location, $rootScope) {
            $rootScope.addLogClass = true;
            $scope.isPageSelected = function (path) {
                var myPath = $location.path();

                // Map plugin-specific pages to the corresponding plugin-type page
                if (myPath.slice(0, "/features".length) == "/features") {
                    myPath = '/installed-features';
                }
                else if (myPath.slice(0, "/tools".length) == "/tools") {
                    myPath = '/tools';
                }

                return  '#' + myPath == path;
            };
        }])
        .controller('navbarCtrl', ['$scope', '$rootScope', '$location',
                          function ($scope,   $rootScope,   $location) {
            $scope.getPageTitle = function() {
                var myPath = $location.path();

                // Map plugin-specific pages to the corresponding plugin-type page
                if (myPath.slice(0, "/features".length) == "/features") {
                    myPath = '/installed-features';
                }
                else if (myPath.slice(0, "/tools".length) == "/tools") {
                    myPath = '/tools';
                }

                var pageMap = {
                    '/introduction': 'Setup',
                    '/library': 'Library',
                    '/installed-features': 'Installed features',
                    '/tools': 'Tools',
                    '/build': 'Rebuild'
                };

                return  pageMap[myPath];
            };
            $rootScope.$on('hide-messages', function() {
                $scope.showMessagesNavbarLink = true;
                $rootScope.addLogClass = !$rootScope.addLogClass;
            });
            $scope.showMessages = function($event) {
                $event.preventDefault();
                $scope.showMessagesNavbarLink = false;
                $rootScope.addLogClass = true;
                $rootScope.$broadcast('show-messages');
            };
            $scope.showMessagesNavbarLink = false;

            $scope.isCollapsed = true;
            $scope.toggleCollapsed = function() {
                $scope.isCollapsed = !$scope.isCollapsed;
            };
        }]);
})();