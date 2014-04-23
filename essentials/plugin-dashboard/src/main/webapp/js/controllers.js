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
            .controller('powerpacksCtrl', function ($scope, $sce, $log, $rootScope, $http, $filter, eventBroadcastService) {
                $scope.hideAll = false;
                $scope.installSampleData = true;
                $scope.includeTemplate = null;
                $scope.resultMessages = null;
                $scope.selectedPlugin = null;
                $scope.powerpackProperties = [];

                $scope.packs = null;
                $scope.buttons = [
                    {buttonText: "Next", previousIndex: 0, nextIndex: 1}
                ];
                $scope.selectedValue = {};
                $scope.stepVisible = [true, false];
                $scope.selectedDescription = "Please make a selection";
                $scope.onPowerpackSelect = function () {
                    angular.forEach($scope.packs, function (powerpack) {
                        var pluginId = powerpack.pluginId;
                        if (pluginId === $scope.selectedValue) {
                            $scope.selectedPlugin = powerpack;
                            $scope.selectedDescription = powerpack.description;
                            $scope.includeTemplate = "/essentials/powerpacks/" + pluginId + "/" + pluginId + ".html"
                        }

                    });

                };

                /**
                 * Powerpack can broadcast properties that
                 * needs to be passed to Powerpack class itself
                 */
                $scope.$on('powerpackEvent', function () {
                    $scope.powerpackProperties = eventBroadcastService.event;
                });

                $scope.onWizardButton = function (idx) {
                    if (idx == 0) {
                        $scope.buttons = [
                            {buttonText: "Next", previousIndex: 0, nextIndex: 1}

                        ];
                        $scope.stepVisible = [true, false];
                    } else {
                        $scope.buttons = [
                            {buttonText: "Previous", previousIndex: 0, nextIndex: 1},
                            {buttonText: "Finish", previousIndex: 0, nextIndex: 2}
                        ];
                        $scope.stepVisible = [false, true];
                    }

                    if (idx == 2) {

                        // execute installation:
                        var payload = Essentials.addPayloadData("pluginId", $scope.selectedPlugin.pluginId, null);
                        angular.forEach($scope.powerpackProperties, function (value) {
                            Essentials.addPayloadData(value.key, value.value, payload);
                        });
                        Essentials.addPayloadData("pluginId", $scope.selectedPlugin.pluginId, null);
                        $http.post($rootScope.REST.powerpacks_install, payload)
                                .success(function (data) {
                                    $scope.resultMessages = data;
                                    $scope.hideAll = true;
                                });
                    }


                };


                $scope.init = function () {
                    if ($scope.initCalled) {
                        return;
                    }

                    $scope.initCalled = true;
                    $http.get($rootScope.REST.plugins).success(function (data) {
                        $scope.packs = [];
                        var powerpacks = $filter('pluginType')("powerpacks", data.items);
                        $scope.packs.push.apply($scope.packs, powerpacks);
                    });

                };
                $scope.init();

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
                            $rootScope.pluginsCache = data.items;
                            processItems(data.items);
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
                            sel =  selected;
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