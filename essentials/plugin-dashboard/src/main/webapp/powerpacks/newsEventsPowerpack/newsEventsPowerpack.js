(function () {
    "use strict";

    angular.module('hippo.essentials')

            .controller('newsEventsCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {




                $scope.init = function () {
                    console.log("LOADED NEWS AND EVENTS");
                };

                $scope.init();

                //############################################
                // DESCRIPTIONS:
                //############################################

            })
})();