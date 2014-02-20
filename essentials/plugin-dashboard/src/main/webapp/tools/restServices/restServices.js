
(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('restServicesCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'restservices/';
                $scope.resultMessages = [];
                $scope.restType = null;
                $scope.selectedType = "Choose REST";
                $scope.restName = "restapi";

                $scope.checkType = function () {
                    console.log($scope.restType);
                };
                $scope.runRestSetup = function () {
                    var payload = Essentials.addPayloadData("restName", $scope.restName, null);
                    Essentials.addPayloadData("restType", $scope.restType, payload);
                    $http.post($scope.endpoint, payload).success(function (data) {
                           // reboot message
                    });
                };
            })
})();