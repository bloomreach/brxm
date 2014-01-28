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
        // loads plugin list
            .controller('pluginCtrl', function ($scope, $location, $sce, $log, $rootScope, $http) {

                $scope.tabs = [
                    {name: "Installed Plugins", link: "/plugins"},
                    {name: "Find additional", link: "/find-plugins"}
                ];
                $scope.isPageSelected = function (path) {
                    return $location.path() == path;
                };
                //plugin list
                $scope.init = function () {
                    $http({
                        method: 'GET',
                        url: $rootScope.REST.plugins
                    }).success(function (data) {
                        console.log("===================");
                        console.log(data);
                        console.log("===================");
                        $scope.plugins = data.items;
                    });

                };
                $scope.init();

            })

        /*
         //############################################
         // ON LOAD CONTROLLER
         //############################################
         */
            .controller('homeCtrl', function ($scope, $sce, $log) {
                $scope.init = function () {
                    $log.info("...Essentials loaded...");
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