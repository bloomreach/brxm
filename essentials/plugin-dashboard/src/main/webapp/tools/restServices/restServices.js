(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('restServicesCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'restservices/';
                $scope.resultMessages = [];
                $scope.restType = null;
                $scope.selectedType = "Choose REST type";
                $scope.restName = "restapi";
                $scope.showDocuments = false;
                $scope.documentTypes = [];

                $scope.init = function () {

                }; $scope.onChangeType = function (restType) {
                    $scope.restType = restType;
                    if(restType=='plain'){
                        // fetch documents:
                        $http.get($scope.endpoint + 'beans').success(function (data) {
                            $scope.documentTypes = data.items;
                        });
                    } else{
                        $scope.documentTypes = [];
                    }
                };
                $scope.runRestSetup = function () {
                    var payload = Essentials.addPayloadData("restName", $scope.restName, null);
                    Essentials.addPayloadData("restType", $scope.restType, payload);
                    $http.post($scope.endpoint, payload).success(function (data) {
                        // TODO: display reboot message and instruction sets executed
                    });
                };
                $scope.init();
            })
})();