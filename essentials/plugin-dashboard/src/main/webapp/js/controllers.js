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
            .controller('pluginLoaderCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

            })
            .controller('powerpacksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
                $scope.hideAll = false;
                $scope.installSampleData = true;
                $scope.includeTemplate = "/essentials/powerpacks/newsEventsPowerpack/newsEventsPowerpack.html";
                $scope.resultMessages = null;

                $scope.packs = null;
                $scope.buttons = [
                    {buttonText: "Next", previousIndex: 0, nextIndex: 1}
                ];
                $scope.selectedValue={};
                $scope.stepVisible = [true, false];
                $scope.selectedDescription = "Please make a selection";
                $scope.onPowerpackSelect = function () {
                    angular.forEach($scope.packs, function (powerpack) {
                        if (powerpack.value === $scope.selectedValue) {
                            $scope.selectedDescription = $scope.getDescription($scope.selectedValue);
                        }
                    });

                };


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
                        $http({
                            method: 'GET',
                            url: $rootScope.REST.powerpacks_install + $scope.selectedItem + "/" + $scope.installSampleData
                        }).success(function (data) {
                            $scope.resultMessages = data;
                            $scope.hideAll = true;
                        });
                    }


                };


                $scope.getDescription = function (name) {
                    if (name.trim() == "news-events") {
                        return  'A basic News and Events site that contains a homepage template, News   \
    and Agenda components and detail pages  \
    to render both News and Event articles. It comes with a standard navigational menu and URL structure. This is the \
    most basic Power Pack to start with. \
    You can easily extend with more components later on.'
                    }
                    return "A REST only site that contains only REST services and no pages.";
                };
                $scope.init = function () {
                    if ($scope.initCalled) {
                        return;
                    }

                    $scope.initCalled = true;
                    $http.get($rootScope.REST.powerpacks).success(function (data) {
                        $scope.packs = [{value: null, enabled: true, name: "Please select a powerpack"}];
                        $scope.packs = [];
                        $scope.packs.push.apply($scope.packs, data.items);

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
                    {name: "Find additional", link: "/find-plugins"}
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


                $scope.showPluginDetail = function (pluginClass) {
                    $scope.selectedPlugin = extracted(pluginClass);
                };
                $scope.installPlugin = function (pluginClass) {
                    $rootScope.pluginsCache = null;
                    $scope.selectedPlugin = extracted(pluginClass);
                    if ($scope.selectedPlugin) {
                        $http.post($rootScope.REST.pluginInstall + pluginClass).success(function (data) {
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
                function extracted(pluginClass) {
                    angular.forEach($scope.plugins, function (selected) {
                        if (selected.pluginClass == pluginClass) {
                            return selected;
                        }
                    });
                    return null;
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