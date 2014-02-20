
(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('restServicesCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'restservices/';
                $scope.resultMessages = [];
                $scope.restType = null;
                $scope.selectedType = "Choose REST";
                $scope.apiName = "restapi";

                $scope.checkType = function () {
                    console.log($scope.restType);
                };
                $scope.runRestSetup = function () {
                    $http.post($scope.endpoint).success(function (data) {

                    });
                };
            })
})();