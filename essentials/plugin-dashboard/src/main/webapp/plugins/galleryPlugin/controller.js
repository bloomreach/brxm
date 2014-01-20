(function () {
    "use strict";

    angular.module('hippo.essentials')
            .controller('galleryPluginCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

                $scope.message = "Gallery plugin";

                $scope.init = function () {
                    $log.info(" **** gallery plugin called ***");
                };

                $scope.init();

            })
}());