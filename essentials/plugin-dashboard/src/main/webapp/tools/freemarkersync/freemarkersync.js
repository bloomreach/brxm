(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('freemarkerSyncCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'freemarkersync/';
                $scope.init = function () {
                    $http.post($scope.endpoint).success(function (data) {
                        console.log(data);
                    });
                };
            })
})();