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
                            controller: 'toolCtrl'
                        })
                        .state('powerpacks', {
                            url: '/powerpacks',
                            templateUrl: 'plugins/newsEventsPowerpack/index.html',
                            controller: 'newsEventsCtrl'
                        })
                        .state('tools-id', {
                            url: '/tools/:id',
                            templateUrl: function ($stateParams) {
                                return 'tools/' + $stateParams.id + '/index.html';
                            },
                            controller: 'toolCtrl'
                        })
                        .state('home', {
                            controller: 'homeCtrl',
                            url: '/home',
                            templateUrl: 'pages/home.html'

                        })
                        .state('plugins', {
                            url: '/plugins',
                            templateUrl: 'pages/plugins.html',
                            controller: 'pluginCtrl',
                            views: {
                                "submenu": {
                                    templateUrl: 'pages/plugins-menu.html',
                                    controller: 'pluginCtrl'
                                }, "plugintabs": {
                                    templateUrl: 'pages/plugins-installed-tabs.html',
                                    controller: 'pluginCtrl'
                                }

                            }

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
                                },"plugininstance":{
                                    url: '/plugins/:id',
                                    templateUrl: function ($stateParams) {
                                        return 'plugins/' + $stateParams.id + '/index.html';
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
            $http.get($rootScope.REST.status + 'powerpack')
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

