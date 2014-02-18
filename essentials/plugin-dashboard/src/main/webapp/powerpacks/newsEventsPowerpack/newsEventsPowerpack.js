(function () {
    "use strict";

    angular.module('hippo.essentials')

            .controller('newsEventsCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {



                $scope.initCalled = false;
                $scope.init = function () {
                    if ($scope.initCalled) {
                        return;
                    }

                    $scope.initCalled = true;
                    $http.get($rootScope.REST.powerpacks).success(function (data) {
                        $scope.packs = data;

                    });

                };

                $scope.init();

                //############################################
                // DESCRIPTIONS:
                //############################################





            })
})();