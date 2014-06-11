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
                            } else {
                                console.log("Nothing selected");
                            }
                        });
                    };


                    //############################################
                    // MODAL
                    //############################################
                    var ModalInstanceCtrl = function ($scope, $modalInstance, endPoint, title, selectedPath) {
                        $scope.title = title;
                        $http.get(endPoint).success(function (data) {
                            console.log("selectedPath" + selectedPath);
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
                    sample: '@'
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
                replace: false,
                restrict: 'E',
                scope: {
                    label: '@',
                    pluginId: '@',
                    pluginTitle: '@',
                    buttonText: '@',
                    hasNoTemplates: '@',
                    sample: '@'
                },
                templateUrl: 'directives/essentials-simple-install-plugin.html',
                controller: function ($scope, $sce, $log, $rootScope, $http) {
                    $scope.showForm = false;

                    $scope.settingsButtonText = $scope.showForm ? "Use these settings" : "Change settings";
                    //$scope.pluginId = pluginId;
                    $scope.sampleData = true;
                    $scope.templateName = 'jsp';
                    $scope.message = {};

                    $scope.payload = {values: {pluginId: $scope.pluginId, sampleData: $scope.sampleData, templateName: $scope.templateName}};
                    $scope.$watchCollection("[sampleData, templateName]", function () {
                        $scope.payload = {values: {pluginId: $scope.pluginId, sampleData: $scope.sampleData, templateName: $scope.templateName}};
                    });
                    $scope.run = function () {
                        $http.post($rootScope.REST.package_install, $scope.payload).success(function (data) {
                            $scope.plugin.addRequested = true;
                            $scope.hideUI = true;
                        });
                    };
                    $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                        $scope.plugin = plugin;
                        $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
                        $scope.hideUI = $scope.plugin.addRequested;
                    });
                }
            }
        })


})();