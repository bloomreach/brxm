(function () {
    "use strict";

    angular.module('hippo.essentials')

            .controller('newsEventsCtrl', function ($scope, $rootScope, eventBroadcastService) {

                $scope.installSampleData = true;
                $scope.onChange = function () {
                    eventBroadcastService.broadcast('powerpackEvent', [
                        {'key': 'sampleDate', 'value': $scope.installSampleData}
                    ])
                };
                $scope.init = function () {

                    // broadcast our item:
                    eventBroadcastService.broadcast('powerpackEvent', [
                        {'key': 'sampleData', 'value': $scope.installSampleData}
                    ])


                };

                $scope.init();

                //############################################
                // DESCRIPTIONS:
                //############################################

            })
})();