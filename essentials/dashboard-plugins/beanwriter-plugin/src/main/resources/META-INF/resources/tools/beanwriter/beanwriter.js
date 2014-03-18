(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('beanWriterCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'beanwriter/';
                $scope.resultMessages = [];
                $scope.runBeanWriter = function () {
                    $http.post($scope.endpoint).success(function (data) {
                        $scope.resultMessages = data;
                    });
                };
            })
})();