(function () {
    "use strict";
    angular.module('hippo.essentials')
        .controller('simpleContentCtrl', function ($scope, $sce, $log, $rootScope, $http) {
            $scope.endpoint = $rootScope.REST.dynamic + 'simpleContent/';
            $scope.sampleData = true;
            $scope.templateName = 'jsp';
            $scope.message = {};
            $scope.run = function () {
                var payload = Essentials.addPayloadData("sampleData", $scope.sampleData, null);
                Essentials.addPayloadData("templateName", $scope.templateName, payload);
                $http.post($scope.endpoint, payload).success(function (data) {
                    // globally handled
                });
            };
        })
})();