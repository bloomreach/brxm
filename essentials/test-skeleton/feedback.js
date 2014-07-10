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
            $scope.plugin = {"restClasses": ["org.onehippo.cms7.essentials.plugins.contentblocks.ContentBlocksResource"], "vendor": {"url": "http://content-blocks.forge.onehippo.org", "name": "Detailed Content Blocks documentation", "logo": null, "introduction": null, "content": null}, "dependencies": [
                {"groupId": "org.onehippo.forge", "artifactId": "content-blocks", "repositoryId": null, "repositoryUrl": null, "version": null, "scope": "compile", "type": "cms", "dependencyType": "CMS"}
            ], "repositories": [], "title": null, "name": "Content Blocks plugin", "introduction": "Provides the ability to add multiple Content Blocks to document types. Each Content Block offers a choice between multiple (potentially complex) field types, as determined by a Provider Compound.", "description": null, "packageClass": null, "packageFile": null, "type": "plugins", "installed": false, "needsInstallation": false, "installState": "boarding", "enabled": true, "dateInstalled": 1404986236235, "documentationLink": null, "libraries": [
                {"prefix": null, "items": [
                    {"browser": null, "component": "contentBlocks", "file": "contentBlocks.js"}
                ]}
            ], "pluginId": "contentBlocks", "issuesLink": null};

            $rootScope.feedbackMessages.push({type: "error", message: "initial message"});
            console.log("messages====================");


        })
        .directive("essentialsPlugin", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'essentials-plugin.html',
                controller: function ($scope, $filter, $sce, $log, $rootScope, $http, $timeout) {

                }
            }
        })
})();