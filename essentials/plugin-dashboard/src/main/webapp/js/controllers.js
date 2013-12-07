/*
 //############################################
 // MENU CONTROLLER
 //############################################

 */
app.controller('mainMenuCtrl', function ($scope, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.menu = [
        {item: "Plugins"},
        {item: "Tools"}
    ];

    $scope.onMenuClick = function (menuItem) {
        $log.info(menuItem);
    };


});