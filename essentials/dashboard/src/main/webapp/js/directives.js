/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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
        .directive("essentialsNotifier", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    messages: '='
                },
                templateUrl: 'directives/essentials-notifier.html',
                controller: function ($scope, $filter, $log, $rootScope, $http, $timeout, $document) {
                    var promisesQueue = [];
                    var lastLength = 0;
                    var ERROR_SHOW_TIME = 1000;
                    $scope.visible = true;
                    $scope.messages = [];

                    $scope.activeMessages = [];
                    $scope.archiveMessages = [$scope.messages[0]];
                    $scope.archiveOpen = false;

                    $scope.$watch('messages', function () {
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
                        if ($scope.archiveMessages.length === 0) {
                            $scope.archiveMessages.push({type: "info", message: 'No archived messages', visible: true, date: now, fullDate: date});
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
                    }, true);

                    $scope.toggleArchive = function ($event) {
                        $scope.archiveOpen = !$scope.archiveOpen;
                        $event.stopPropagation();
                    };
                    $scope.hide = function() {
                        $scope.visible = false;
                        $rootScope.$broadcast('hide-messages');
                    };
                    $rootScope.$on('show-messages', function() {
                        $scope.visible = true;
                    });
                    $scope.toggleErrors = function ($event) {
                        $event.stopPropagation();
                        $scope.showErrors = !$scope.showErrors;
                    };

                    $document.bind('click', function () {
                        $scope.archiveOpen = false;
                        $scope.$apply();
                    });
                }
            };
        }).directive("essentialsPlugin", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-plugin.html',
                controller: function ($scope, $filter, $log, $rootScope, $http, $location, pluginService) {
                    $scope.slides = [];
                    angular.forEach($scope.plugin.imageUrls, function(url) {
                        $scope.slides.push({
                            image: url
                        });
                    });
                    $scope.interval = 5000; // carousel change interval
                    $scope.showDescription = false;
                    $scope.toggleDescription = function ($event) {
                        $event.preventDefault();
                        $scope.showDescription = !$scope.showDescription;
                    };
                    $scope.installPlugin = function () {
                        $scope.installButtonDisabled = true; // avoid double-clicking

                        // If the new state of the plugin (available if the promise resolves successfully) is
                        // 'awaitingUserInput', we switch the view to the plugin's page, where the user
                        // is prompted for input.
                        pluginService.install($scope.plugin.id).then(function() {
                            if ($scope.plugin.installState === 'awaitingUserInput') {
                                $location.path('/features/' + $scope.plugin.id);
                            }
                            // else we stay in the current state so more features can be installed.
                        });
                    };
                }
            };
        }).directive("essentialsInstalledFeature", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-feature.html'
            };
        }).directive("essentialsInstalledTool", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-tool.html',
                controller: function ($scope, $location) {
                    $scope.useTool = function () {
                        $location.path('/tools/' + $scope.plugin.id);
                    };
                }
            };
        }).directive("essentialsFeatureFooter", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-feature-footer.html',
                controller: function ($scope, $rootScope, pluginService) {
                    $scope.toggleChanges = function($event) {
                        $event.preventDefault();
                        $scope.showChanges = !$scope.showChanges;

                        // Lazy-loading messages
                        if (!$scope.messagesLoaded) {
                            pluginService.getChangeMessages($scope.plugin.id).then(function(changeMessages) {
                                $scope.changeMessages = changeMessages;
                                $scope.messagesLoaded = true;
                            });
                        }
                    };

                    $scope.messagesLoaded = false; // Flag for lazy loading
                    $scope.showChanges = false;
                    $scope.hasMessages = !!$scope.plugin.packageFile;
                }
            };
        }).directive("essentialsFolderPicker", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    title: '@',
                    buttonText: '@',
                    selectedPath: '=',
                    selected: '='
                },
                templateUrl: 'directives/essentials-folder-picker.html',
                controller: function ($scope, $uibModal, $log, $http) {
                    $scope.open = function (size) {
                        var modalInstance = $uibModal.open({
                            templateUrl: 'tree-picker.html',
                            controller: ModalInstanceCtrl,
                            size: size,
                            resolve: {
                                title: function () {
                                    return $scope.title;
                                }, buttonText: function () {
                                    return $scope.buttonText;
                                }, selectedPath: function () {
                                    return $scope.selectedPath;
                                }, selected: function () {
                                    return $scope.selected;
                                }
                            }

                        });
                        modalInstance.result.then(function (selected) {
                            if (selected) {
                                $scope.selected = selected;
                                $scope.selectedPath = selected.id;
                            }
                        });
                    };

                    var ModalInstanceCtrl = function ($scope, $uibModalInstance, title) {
                        $scope.title = title;
                        $http.get(window.SERVER_URL + '/essentials/rest/jcrbrowser/folders').then(function (response) {
                            $scope.treeItems = response.data.items;
                        });
                        $scope.ok = function () {
                            $uibModalInstance.close($scope.selected);
                        };
                        $scope.cancel = function () {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.callbacks = {
                            accept: function () {
                                // disable drag/drop stuff
                                return false;
                            },
                            dragStart: function (event) {
                                $scope.selected = event.source.nodeScope.$modelValue;
                                $scope.selectedPath = $scope.selected.id;
                            },
                            dragStop: function (event) {
                                // noop
                            },
                            dropped: function (event) {
                                // noop
                            }
                        };
                    };
                }
            };
        });
})();
