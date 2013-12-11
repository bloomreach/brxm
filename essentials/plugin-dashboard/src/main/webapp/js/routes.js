//############################################
// ROUTES
//############################################
// configure our routes
app.config(function ($routeProvider) {

    $routeProvider
            .when('/', {
                templateUrl: 'pages/home.html',
                controller: 'mainCtrl',
                resolve: {
                    factory: checkPackInstalled
                }
            })
            .when('/powerpacks', {
                templateUrl: 'plugins/newsEventsPowerpack/index.html',
                controller: 'newsEventsCtrl'
            }).when('/plugins', {
                templateUrl: 'pages/plugins.html',
                controller: 'pluginCtrl'
            }).when('/find-plugins', {
                templateUrl: 'pages/find-plugins.html',
                controller: 'pluginCtrl'
            }).when('/tools', {
                templateUrl: 'pages/tools.html',
                controller: 'toolCtrl'
            })
        //############################################
        // PLUGINS: TODO make dynamic
        //############################################
            .when('/plugins/:pluginId',
            {
                templateUrl: function (params) {
                    return 'plugins/' + params.pluginId + '/index.html';
                },
                controller: 'pluginLoaderCtrl'

            })
            .otherwise({redirectTo: '/'})

});

var checkPackInstalled = function ($q, $rootScope, $location, $http, $log, MyHttpInterceptor) {

  /*  if (true) {
        // TODO mm: enable
        $rootScope.packsInstalled = true;
        return true;
    }*/

    if ($rootScope.packsInstalled) {
        $log.info("powerpack is installed");
        return true;
    } else {
        var deferred = $q.defer();
        $http.get($rootScope.REST.status + '/powerpack')
                .success(function (response) {
                    $rootScope.packsInstalled = response.packsInstalled;
                    deferred.resolve(true);
                })
                .error(function () {
                    deferred.reject();
                    $location.path("/powerpacks");
                });
        return deferred.promise;
    }
};