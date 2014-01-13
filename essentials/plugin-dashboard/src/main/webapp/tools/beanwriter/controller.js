app.controller('beanWriterCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
    $scope.resultMessages = [];
    $scope.runBeanWriter = function () {
        $http({
            method: 'POST',
            url: $rootScope.REST.beanwriter
        }).success(function (data) {
                    $scope.resultMessages = data;
                });

    };
});