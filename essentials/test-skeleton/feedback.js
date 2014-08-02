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
            $scope.plugin = {
                "restClasses": [],
                "vendor": {
                    "url": "http://www.onehippo.com",
                    "name": "Hippo",
                    "logo": null,
                    "introduction": null,
                    "content": null
                },
                "dependencies": [
                    {
                        "groupId": "org.onehippo.forge.robotstxt",
                        "artifactId": "robotstxt-addon-repository",
                        "repositoryId": null,
                        "repositoryUrl": null,
                        "version": null,
                        "scope": "compile",
                        "type": "cms",
                        "dependencyType": "CMS"
                    },
                    {
                        "groupId": "org.onehippo.forge.robotstxt",
                        "artifactId": "robotstxt-hst-client",
                        "repositoryId": null,
                        "repositoryUrl": null,
                        "version": null,
                        "scope": "compile",
                        "type": "site",
                        "dependencyType": "SITE"
                    }
                ],
                "repositories": [],
                "title": null,
                "name": "Robots plugin",
                "introduction": "Add Robots plugin support",
                "description": "The robotstxt plugin adds a special document type to the CMS, allowing webmasters to determine the content of the robots.txt file retrieved by webbots. See <a href=\"http://www.robotstxt.org/\">robotstxt.org</a> for more information on the format and purpose of that file.<p>The robotstxt plugin provides Beans and Components for retrieving the robots.txt-related data from the Hippo repository, and a sample JSP file for rendering that data into the robots.txt file.</p>\n<p>Read more about Robots.txt plugin at plugin site: <a href=\"http://robotstxt.forge.onehippo.org/\" target=\"_blank\">http://robotstxt.forge.onehippo.org/</a></p>",
                "packageClass": null,
                "packageFile": "/META-INF/robots_plugin_instructions.xml",
                "type": "feature",
                "installed": false,
                "needsInstallation": false,
                "installState": "boarding",
                "enabled": true,
                "dateInstalled": 1405070530039,
                "documentationLink": null,
                "libraries": [],
                "id": "robotsPlugin",
                "issuesLink": null
            };

            $rootScope.feedbackMessages.push({type: "error", message: "initial message"});



        }).directive("essentialsPluginTest", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '=',
                    plugins: '='
                },
                templateUrl: 'directives/essentials-plugin.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http, $timeout) {
                    $scope.installPlugin = function (pluginId) {
                        $rootScope.pluginsCache = null;
                        $scope.selectedPlugin = extracted(pluginId);
                        if ($scope.selectedPlugin) {
                            $http.post($rootScope.REST.pluginInstall + pluginId).success(function (data) {
                                // we'll get error message or
                                //$scope.init();
                            });
                        }
                    };
                    var p = $scope.plugin;
                    // set install state:
                    if (p) {
                        $scope.showRebuildMessage = p.installState === 'installing';
                        $scope.showInstalledMessage = p.installState === 'installed';
                        $scope.showBoardingMessage = p.installState === 'boarding';
                        $scope.showPlugin = !($scope.showRebuildMessage || $scope.showInstalledMessage || $scope.showBoardingMessage);
                    }
                    function extracted(pluginId) {
                        var sel = null;
                        angular.forEach($scope.plugins, function (selected) {
                            if (selected.id == pluginId) {
                                sel = selected;
                            }
                        });
                        return sel;
                    }

                }
            }
        })

})();