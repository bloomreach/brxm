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
    angular.module('hippo.essentials')

        //############################################
        // MENU DATA
        //############################################
        .service('menuService', function () {
            this.getMenu = function () {
                return [
                    {name: "Plugins", link: "#/plugins"},
                    {name: "Tools", link: "#/tools"}
                ];
            };

        })

        //############################################
        // PLUGINS CONTROLLER LOADER
        //############################################
        .controller('pluginLoaderCtrl', function ($scope, $sce, $log, $rootScope, $http, $filter) {

        })
        .controller('introductionCtrl', function ($scope, $location, $sce, $log, $rootScope, $http) {
            // just sets a hide screen boolean flag to true
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
                $http.post($rootScope.REST.save_settings, $scope.projectSettings).success(function (data) {
                    window.location = "/essentials";
                });

            }
            $scope.setup = function () {
                $http.get($rootScope.REST.project_settings).success(function (data) {
                    $scope.projectSettings = data;
                    // set some defaults
                    $scope.projectSettings.templateLanguage = 'jsp';
                    $scope.projectSettings.useSamples = true;
                });
            }
            // initialize
            $scope.setup();

        })
        .controller('pluginCtrl', function ($scope, $location, $sce, $log, $rootScope, $http) {

            $scope.allPluginsInstalled = "No additional plugins could be found";
            $scope.plugins = [];
            $scope.pluginNeedsInstall = [];
            $scope.selectedPlugin = null;
            $scope.tabs = [
                {name: "Installed Plugins", link: "/plugins"},
                {name: "Find", link: "/find-plugins"}
            ];
            $scope.isPageSelected = function (path) {
                return $location.path() == path;
            };
            $scope.isTabSelected = function (path) {
                var myPath = $location.path();
                return  myPath.indexOf(path) != -1;
            };
            $scope.isMenuSelected = function (path) {
                var myPath = $location.path();
                return  myPath.indexOf(path) != -1;
            };


            $scope.showPluginDetail = function (pluginId) {
                $scope.selectedPlugin = extracted(pluginId);
                $rootScope.selectedPlugin = extracted(pluginId);
            };
            $scope.installPlugin = function (pluginId) {
                $rootScope.pluginsCache = null;
                $scope.selectedPlugin = extracted(pluginId);
                if ($scope.selectedPlugin) {
                    $http.post($rootScope.REST.pluginInstall + pluginId).success(function (data) {
                        // we'll get error message or
                        $scope.init();
                    });
                }
            };


            //fetch plugin list
            $scope.init = function () {

                if ($rootScope.pluginsCache) {
                    processItems($rootScope.pluginsCache);
                } else {
                    $http.get($rootScope.REST.plugins).success(function (data) {
                        var items = data.items;
                        $rootScope.pluginsCache = items;
                        processItems(items);
                        // fetch remote repositories:
                        console.log(">>> LOCAL <<<");
                        fetchRemote(items)
                        function fetchRemote(items) {

                            if(!items){
                                items = [];
                            }
                            $http.get($rootScope.REST.plugins  + "remote").success(function (data) {
                                console.log("==========================================");
                                console.log("==========================================");
                                console.log("==========================================");
                                console.log(" >>> remote data");
                                console.log(data.items);
                                items.concat(data.items);
                                console.log("+++++++++++++++++++++++++++++++++++++++===");
                                var all = items.concat(data.items);
                                console.log(all);
                                $rootScope.pluginsCache = all;
                                processItems(items);
                            });
                        }


                    });


                }

                function processItems(items) {
                    $scope.plugins = items;
                    $scope.pluginNeedsInstall = [];
                    angular.forEach(items, function (obj) {
                        if (obj.needsInstallation) {
                            $scope.pluginNeedsInstall.push(obj);
                        }
                    });
                }
            };
            $scope.init();
            function extracted(pluginId) {
                var sel = null;
                angular.forEach($scope.plugins, function (selected) {
                    if (selected.pluginId == pluginId) {
                        sel = selected;
                    }
                });
                return sel;
            }
        })

        /*
         //############################################
         // ON LOAD CONTROLLER
         //############################################
         */
        .controller('homeCtrl', function ($scope, $http, $rootScope) {
            $scope.plugins = [];
            $scope.init = function () {
                $http.get($rootScope.REST.plugins).success(function (data) {
                    $scope.plugins = [];
                    var items = data.items;
                    angular.forEach(items, function (plugin) {
                        if (plugin.dateInstalled) {
                            $scope.plugins.push(plugin);
                        }
                    });
                });

            };
            $scope.init();

        })

        /*
         //############################################
         // MENU CONTROLLER
         //############################################
         */
        .controller('mainMenuCtrl', ['$scope', '$location', '$rootScope', 'menuService', function ($scope, $location, $rootScope, menuService) {

            $scope.$watch(function () {
                return $rootScope.busyLoading;
            }, function () {
                $scope.menu = menuService.getMenu();
            });

            $scope.isPageSelected = function (path) {
                var myPath = $location.path();
                // stay in plugins for all /plugin paths
                if (myPath == "/find-plugins" || myPath.indexOf("/plugins") != -1) {
                    myPath = '/plugins';
                }
                else if (myPath.slice(0, "/tools".length) == "/tools") {
                    myPath = '/tools';
                }
                return  '#' + myPath == path;
            };

            $scope.onMenuClick = function (menuItem) {
                //
            };

        }]);

})();