/*
 //############################################
 // MENU CONTROLLER
 //############################################

 */
app.controller('mainCtrl', function ($scope, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.message = "Welcome";
    $scope.packs = {"powerpacks": [
        {"enabled": true, "name": "Basic News and Events site"},
        {"enabled": false, "name": "A REST only site that contains only REST services and no pages."}
    ], "project": {"namespace": "marketplace"}, "steps": [
        {"name": "Select a powerpack"},
        {"name": "Validate"}
    ]};


});

app.controller('mainMenuCtrl', function ($scope, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.menu = [
        {item: "Plugins"},
        {item: "Tools"}
    ];

    $scope.onMenuClick = function (menuItem) {
        $log.info(menuItem);
    };


});