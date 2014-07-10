/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    "use strict";
    angular.module('hippo.essentials')
        .controller('feedbackCtrl', function ($scope, $filter, $sce, $log, $rootScope, $http, $timeout) {
            var promisesQueue = [];
            var lastLength = 0;
            var ERROR_SHOW_TIME = 3000;
            $scope.messages = [];

            $scope.activeMessages = [];
            $scope.archiveMessages = [$scope.messages[0]];



            $scope.toggleArchive = function () {
                $scope.archiveOpen = !$scope.archiveOpen;
            };

            $scope.generateFew = function () {
                var tmp = [];
                var rand = generateRandom(10);
                for (var i = 0; i < rand; i++) {
                    tmp.push(genMessage());
                }
                $scope.messages = $scope.messages.concat(tmp);
            };

            $scope.generateMessage = function () {
                $scope.messages.push(genMessage());
            };


            //############################################
            // TEMP
            //############################################

            function generateRandom(max) {
                return Math.round(Math.random() * (max - 1) + 1);
            }

            function genMessage() {
                var getType = function (r) {
                    if (r % 2) {
                        return "info"
                    }
                    if (generateRandom(300) > 100) {
                        return "error";
                    }
                    if (!(r & 1)) {
                        return "warning"
                    }
                    return "error"
                };
                return {visible: true, message: "Feedback Message " + generateRandom(1000), type: getType(generateRandom(1000))};
            }

        })
        .directive("testEssentialsNotifier", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    messages: '='
                },
                templateUrl: 'essentials-notifier.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http, $timeout) {
                    var promisesQueue = [];
                    var lastLength = 0;
                    var ERROR_SHOW_TIME = 3000;
                    $scope.messages = [];

                    $scope.activeMessages = [];
                    $scope.archiveMessages = [$scope.messages[0]];
                    $scope.archiveOpen = true;

                    var watcher = function (force) {
                        if(force===true){
                            lastLength--;
                        }
                        // don't execute if message count is not changed, e.g. when changing visibility only
                        if (lastLength == $scope.messages.length) {
                            return;
                        }

                        var date = new Date();
                        var now = date.toLocaleTimeString();
                        // cancel all hide promises
                        angular.forEach(promisesQueue, function (promise) {
                            $timeout.cancel(promise);
                        });
                        promisesQueue = [];
                        // keep messages which are not older than time showed + ERROR_SHOW_TIME:
                        /*  var elapsedTime = new Date();
                         elapsedTime.setSeconds(elapsedTime.getSeconds() + ERROR_SHOW_TIME);
                         var keepValuesCounter =0;
                         angular.forEach($scope.activeMessages, function (value) {
                         if(value.fullDate && value.fullDate.getDate() < elapsedTime){
                         keepValuesCounter++;
                         }
                         });*/
                        var currentLength = $scope.messages.length;
                        var startIdx = lastLength;
                        lastLength = currentLength;
                        $scope.activeMessages = [];
                        $scope.activeMessages = $scope.messages.slice(startIdx, currentLength);
                        $scope.archiveMessages = $scope.messages.slice(0, startIdx);

                        angular.forEach($scope.messages, function (value) {
                            value.visible = true;
                            if (!value.date) {
                                value.date = now;
                                value.fullDate = date;
                            }
                        });
                        if ($scope.archiveMessages.length == 0) {
                            $scope.archiveMessages.push({type: "info", message: 'No archived messages', visible: true, date: now, fullDate: date})
                        }
                        // newer messages first:
                        $scope.archiveMessages.reverse();
                        if ($scope.activeMessages.length > 1) {
                            // animate close:
                            var counter = 1;
                            var copy = $scope.activeMessages.slice(0);
                            angular.forEach(copy, function (value) {
                                if (counter > 1) {
                                    var promise = $timeout(function () {
                                        value.visible = false;
                                        $scope.archiveMessages.unshift(value);
                                    }, ERROR_SHOW_TIME * counter);
                                    promisesQueue.push(promise);
                                }
                                counter = counter + 0.5;
                            });
                        }
                    };
                    $scope.$watch('messages', watcher, true);


                    $scope.toggleArchive = function () {
                        $scope.archiveOpen = !$scope.archiveOpen;
                    };

                    $scope.archive = function () {
                        watcher(true);
                    };


                }
            }
        })
})();