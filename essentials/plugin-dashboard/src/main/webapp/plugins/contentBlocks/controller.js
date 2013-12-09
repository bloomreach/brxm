app.controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.welcomeMessage = "Content blocks plugin";
    $scope.init = function(){
        $log.warn("contentBlocksCtrl is invoked");
    };
    $scope.init();
});