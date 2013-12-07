/*
 //############################################
 // MENU CONTROLLER
 //############################################

 */
app.controller('mainCtrl', function ($scope, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.message = "Welcome";


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