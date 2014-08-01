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
        //############################################
        // DIRECTIVES
        //############################################
        .directive("essentialsFolderPicker", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    title: '@',
                    buttonText: '@',
                    endPoint: '@',
                    selectedPath: '=',
                    selected: '='
                },
                templateUrl: 'directives/essentials-folder-picker.html',
                controller: function ($scope, $rootScope, $modal, $log, $http) {
                    $scope.open = function (size) {
                        var modalInstance = $modal.open({
                            templateUrl: 'tree-picker.html',
                            controller: ModalInstanceCtrl,
                            size: size,
                            resolve: {
                                endPoint: function () {
                                    return $scope.endPoint;
                                }, title: function () {
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


                    //############################################
                    // MODAL
                    //############################################
                    var ModalInstanceCtrl = function ($scope, $modalInstance, endPoint, title, selectedPath) {
                        $scope.title = title;
                        $http.get(endPoint).success(function (data) {
                            $scope.treeItems = data.items;
                        });
                        $scope.ok = function () {
                            $modalInstance.close($scope.selected);
                        };

                        $scope.cancel = function () {
                            $modalInstance.dismiss('cancel');
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
            }
        }).directive("essentialsTemplateSettings", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    label: '@',
                    /* settings: '=',*/
                    sampleData: '=',
                    templateName: '=',
                    hasNoTemplates: '@',
                    hasSampleData: '@'
                },
                templateUrl: 'directives/essentials-template-settings.html',
                controller: function ($scope, $rootScope, $http) {
                    $scope.toggleForm = function () {
                        $scope.showForm = !$scope.showForm;
                    };
                    $http.get($rootScope.REST.project_settings).success(function (data) {
                        $scope.projectSettings = data;
                        $scope.templateName = $scope.projectSettings.templateLanguage;
                        $scope.sampleData = $scope.projectSettings.useSamples;
                    });

                }
            }
        })
        .directive("essentialsHelp", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    helpText: '@',
                    helpReference: '=',
                    showHideVariable: '='
                },
                templateUrl: 'directives/essentials-help.html',
                controller: function ($scope) {
                    $scope.text = $scope.helpText ? $scope.helpText : $scope.helpReference;
                }
            }
        })
        .directive("essentialsMessages", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    label: '@',
                    payload: '='
                },
                templateUrl: 'directives/essentials-messages.html',
                controller: function ($scope, installerFactory) {
                    // refresh messages when changes are made:
                    $scope.$watch('payload', function (newValue) {
                        if (newValue) {
                            return installerFactory.packageMessages(newValue).success(function (data) {
                                return $scope.packageMessages = data;
                            });
                        }
                    }, true);
                    return installerFactory.packageMessages($scope.payload).success(function (data) {
                        return $scope.packageMessages = data;
                    });
                }
            }
        }).directive("essentialsSimpleInstallPlugin", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    label: '@',
                    pluginId: '@',
                    hasNoTemplates: '@',
                    hasSampleData: '@',
                    hideInstalledInfo: '@',
                    requireOnBoardState: '@' // use if CMS dependency bootstraps namespace required during installation.
                },
                templateUrl: 'directives/essentials-simple-install-plugin.html',
                controller: function ($scope, $sce, $log, $location, $rootScope, $http) {
                    $scope.showForm = false;

                    $scope.settingsButtonText = $scope.showForm ? "Use these settings" : "Change settings";
                    $scope.sampleData = true;
                    $scope.templateName = 'jsp';
                    $scope.message = {};

                    $scope.payload = {values: {pluginId: $scope.pluginId, sampleData: $scope.sampleData, templateName: $scope.templateName}};
                    $scope.$watchCollection("[sampleData, templateName]", function () {
                        $scope.payload = {values: {pluginId: $scope.pluginId, sampleData: $scope.sampleData, templateName: $scope.templateName}};
                    });
                    $scope.run = function () {
                        $http.post($rootScope.REST.package_install, $scope.payload).success(function (data) {
                            $scope.plugin.installState = 'installing';
                            $log.debug('signalling state change for plugin ' + $scope.plugin.pluginId);
                            $rootScope.$broadcast('update-plugin-install-state', {
                                'pluginId': $scope.pluginId,
                                'state': $scope.plugin.installState
                            });
                            $location.path('/installed-features');
                        });
                    };
                    $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                        $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
                        $scope.plugin = plugin;
                    });
                }
            }
        }).directive("essentialsCmsDocumentTypeDeepLink", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    nameSpace: '@',
                    documentName: '@',
                    label: '@'
                },
                templateUrl: 'directives/essentials-cms-document-type-deep-link.html',
                controller: function ($scope, $sce, $log, $rootScope, $http) {
                    $scope.label = 'CMS Document Type Editor';
                    $scope.defaultNameSpace = $rootScope.projectSettings.namespace;
                }
            }
        }).directive("essentialsNotifier", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    messages: '='
                },
                templateUrl: 'directives/essentials-notifier.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http, $timeout) {
                    var promisesQueue = [];
                    var lastLength = 0;
                    var ERROR_SHOW_TIME = 3000;
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
                    }, true);

                    $scope.toggleArchive = function () {
                        $scope.archiveOpen = !$scope.archiveOpen;
                    };
                }
            }
        }).directive("essentialsPlugin", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-plugin.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http) {
                    var displayTypeMap = {
                        'plugins' : 'feature',
                        'tools'   : 'tool'
                    };
                    $scope.getDisplayType = function() {
                        return displayTypeMap[$scope.plugin.type];
                    };
                    $scope.isDiscovered = function() {
                        return $scope.plugin.type === 'plugins' && $scope.plugin.installState === 'discovered';
                    };
                    $scope.isBoarding = function() {
                        return $scope.plugin.installState === 'boarding';
                    };
                    $scope.isOnBoard = function() {
                        return $scope.plugin.type === 'tools'
                               || ($scope.plugin.installState !== 'discovered' && $scope.plugin.installState !== 'boarding');
                    };
                    $scope.installPlugin = function () {
                        $rootScope.pluginsCache = null;
                        var pluginId = $scope.plugin.pluginId;
                        $http.post($rootScope.REST.pluginInstall + pluginId).success(function (data) {
                            // reload because of install state change:
                            $http.get($rootScope.REST.plugins +'plugins/' + pluginId).success(function (data) {
                                $scope.plugin = data;
                            });
                        });
                    };
                }
            }
        }).directive("essentialsInstalledFeature", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-feature.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http) {
                    $scope.needsRebuild = function() {
                        var state = $scope.plugin.installState;
                        return state === 'boarding' || state === 'installing';
                    };
                    $scope.needsConfiguration = function() {
                        return $scope.plugin.installState === 'onBoard';
                    };
                    $scope.hasConfiguration = function() {
                        return $scope.plugin.installState === 'installed' && $scope.plugin.hasConfiguration;
                    };
                    $scope.hasNoConfiguration = function() {
                        return $scope.plugin.installState === 'installed' && !$scope.plugin.hasConfiguration;
                    };
                }
            }
        }).directive("essentialsInstalledTool", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '=',
                    plugins: '='
                },
                templateUrl: 'directives/essentials-installed-tool.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http) {
                    $scope.installPlugin = function (pluginId) {
                        $rootScope.pluginsCache = null;
                        $scope.selectedPlugin = extracted(pluginId);
                        if ($scope.selectedPlugin) {
                            $http.post($rootScope.REST.pluginInstall + pluginId).success(function (data) {
                                // reload because of install state change:
                                $http.get($rootScope.REST.plugins +'plugins/' + pluginId).success(function (data) {
                                    $scope.plugin = data;
                                    initializeInstallState($scope.plugin);
                                });
                            });
                        }
                    };
                    initializeInstallState($scope.plugin);
                    function initializeInstallState(p){

                        // set install state:
                        if (p) {
                            $scope.showRebuildMessage = p.installState === 'installing';
                            $scope.showInstalledMessage = p.installState === 'installed';
                            $scope.showBoardingMessage = p.installState === 'boarding';
                            $scope.showPlugin = !($scope.showRebuildMessage || $scope.showInstalledMessage || $scope.showBoardingMessage);
                        }
                    }
                    function extracted(pluginId) {
                        var sel = null;
                        angular.forEach($scope.plugins, function (selected) {
                            if (selected.pluginId == pluginId) {
                                sel = selected;
                            }
                        });
                        return sel;
                    }

                }
            }
        })

})();