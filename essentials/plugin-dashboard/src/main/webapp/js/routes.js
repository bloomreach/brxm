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
                templateUrl: 'pages/powerpacks.html',
                controller: 'mainCtrl'
            }).when('/plugin/:id', {
                templateUrl: 'pages/powerpacks.html',
                controller: 'mainCtrl'
            })
            .otherwise({redirectTo: '/'})

});
var checkPackInstalled = function ($q, $rootScope, $location, $http, $log) {
    if ($rootScope.packsInstalled) {
        $log.info("powerpack is installed");
        return true;
    } else {
        var deferred = $q.defer();
        $http.get($rootScope.REST.status +'/powerpack')
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