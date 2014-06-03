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
        .directive("essentialsTemplateSettings", function () {
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
                        });
                    };
                    $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                        $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
                    });
                }

            }
        })


})();