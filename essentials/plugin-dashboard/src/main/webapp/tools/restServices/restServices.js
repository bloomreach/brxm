
(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('restServicesCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'restservices/';
                $scope.resultMessages = [];
                $scope.runRestSetup = function () {
                    $http.post($scope.endpoint).success(function (data) {

                    });
                };
            })
})();