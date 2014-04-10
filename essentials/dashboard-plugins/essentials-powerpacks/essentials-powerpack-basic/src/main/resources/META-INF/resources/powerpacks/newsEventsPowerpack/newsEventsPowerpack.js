(function () {
    "use strict";

    angular.module('hippo.essentials')

            .controller('newsEventsCtrl', function ($scope, $rootScope, eventBroadcastService) {

                $scope.sampleData = true;
                $scope.onChange = function () {
                    eventBroadcastService.broadcast('powerpackEvent', [
                        {'key': 'sampleData', 'value': $scope.sampleData}
                    ])
                };
                $scope.init = function () {

                    // broadcast our item:
                    eventBroadcastService.broadcast('powerpackEvent', [
                        {'key': 'sampleData', 'value': $scope.sampleData}
                    ])


                };

                $scope.init();

                //############################################
                // DESCRIPTIONS:
                //############################################

            })
})();