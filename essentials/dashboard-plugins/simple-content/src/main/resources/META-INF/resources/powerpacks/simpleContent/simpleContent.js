(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('simpleContentCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'simpleContent/';
                $scope.message = {};
                $scope.run = function() {
                                                    $http.post($scope.endpoint).success(function (data) {
                            $scope.message = data;
                                            });
                };
            })
})();