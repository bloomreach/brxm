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
            .controller('pluginLoaderCtrl',function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

            }).controller('toolCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
                // does nothing for time being
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
                    $scope.selectedPlugin  = extracted(pluginClass);
                };
                $scope.installPlugin = function (pluginClass) {
                    $rootScope.pluginsCache = null;
                    $scope.selectedPlugin  = extracted(pluginClass);
                    if($scope.selectedPlugin){
                        $http.post($rootScope.REST.pluginInstall+pluginClass).success(function (data) {
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
                        $scope.plugins =  [];
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
            .controller('mainMenuCtrl', ['$scope', '$location', '$rootScope',  'menuService', function ($scope, $location, $rootScope, menuService) {

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