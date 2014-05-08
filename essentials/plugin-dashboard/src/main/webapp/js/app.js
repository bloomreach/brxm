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

/**
 * NOTE: when creating classes which are part of the "hippo.essentials" module,
 * do not include dependnecies like below, but add them to this (app.js) module). An example:
 * <pre>
 (function () {
    "use strict";
    angular.module('hippo.essentials')
    })();
 * </pre>
 *
 * Note that dependencies are missing (['ngRoute', 'localytics.directives'] etc.)
 *
 */
(function () {
    "use strict";

    angular.module('hippo.essentials', ['ngRoute', 'localytics.directives', 'ui.validate', 'ui.bootstrap', 'ui.sortable', 'ui.router'])

//############################################
// GLOBAL LOADING
//############################################
        .config(function ($provide, $httpProvider, $controllerProvider, $compileProvider) {

            $provide.factory('MyHttpInterceptor', function ($q, $rootScope, $log) {
                return {
                    //############################################
                    // REQUEST
                    //############################################
                    request: function (config) {
                        if (!$rootScope.FEEDBACK_TIMER) {
                            $rootScope.FEEDBACK_TIMER = new Date();
                        }
                        $rootScope.busyLoading = true;
                        var date = new Date();
                        // keep success messages for 5 seconds
                        if ((date.getTime() - $rootScope.FEEDBACK_TIMER.getTime()) > 5000) {
                            $rootScope.FEEDBACK_TIMER = new Date();
                            $rootScope.feedbackMessages = [];
                        }
                        return config || $q.when(config);
                    },
                    requestError: function (error) {
                        $rootScope.busyLoading = true;
                        $rootScope.globalError = [];
                        $rootScope.feedbackMessages = [];
                        if (error.data) {
                            $rootScope.globalError.push(error.data);
                        }
                        else {
                            $rootScope.globalError.push(error.status);
                        }
                        return $q.reject(error);
                    },

                    //############################################
                    // RESPONSE
                    //############################################
                    response: function (data) {
                        $rootScope.busyLoading = false;
                        $rootScope.globalError = [];
                        // show success message:
                        if (data.data.successMessage) {
                            $rootScope.globalError = [];
                            $rootScope.feedbackMessages = [];
                            $rootScope.feedbackMessages.push(data.data.value);
                        }
                        $log.info(data);
                        return data || $q.when(data);
                    },
                    responseError: function (error) {
                        $rootScope.busyLoading = false;
                        $rootScope.globalError = [];
                        $rootScope.feedbackMessages = [];
                        if (error.data) {
                            $rootScope.globalError.push(error.data);
                        }
                        else {
                            $rootScope.globalError.push(error.status);
                        }
                        $log.error(error);
                        return $q.reject(error);
                    }
                };
            });
            $httpProvider.interceptors.push('MyHttpInterceptor');
        })

//############################################
// RUN
//############################################


        .run(function ($rootScope, $location, $log, $http, $state) {
            $rootScope.headerMessage = "Welcome on the Hippo Trail";
            // routing listener
            $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams) {
                // noop

            });


            var root = 'http://localhost:8080/essentials/rest';
            var plugins = root + "/plugins";

            $rootScope.PLUGIN_GROUP = {
                plugin: "plugins",
                tool: "tools"
            };
            /* TODO generate this server side ?*/
            $rootScope.REST = {
                root: root,
                menus: root + '/menus/',
                jcr: root + '/jcr/',
                jcrQuery: root + '/jcr/query/',
                dynamic: root + '/dynamic/',

                /**
                 * Returns list of all plugins
                 * //TODO: change this once we have marketplace up and running
                 */
                plugins: root + "/plugins/",
                projectSettings: plugins + '/settings',
                packageStatus: plugins + '/status/package/',
                packageMessages: plugins + '/changes/',
                controllers: plugins + '/controllers/',
                /**
                 * Returns list of all plugins that need configuration
                 */
                pluginsToConfigure: plugins + '/configure/list/',
                /**
                 *Add a plugin to the list of plugins that need configuration:
                 * POST method
                 * DELETE method deletes plugin from the list
                 */
                pluginsAddToConfigure: plugins + '/configure/add/',
                /**
                 *
                 * /installstate/{className}
                 */
                pluginInstallState: plugins + '/installstate/',
                /**
                 *  * /install/{className}
                 */
                pluginInstall: plugins + '/install/',
                /**
                 * Returns a list of plugin modules (javascript includes)
                 */
                pluginModules: plugins + '/modules/',


                documentTypes: root + '/documenttypes/',

                package_install: plugins + '/install/package',
                hide_introduction: plugins + '/hideintroduction',

                compounds: root + '/documenttypes/compounds',
                compoundsCreate: root + '/documenttypes/compounds/create/',
                compoundsDelete: root + '/documenttypes/compounds/delete/',
                contentblocksCreate: root + '/documenttypes/compounds/contentblocks/create/',
                //############################################
                // NODE
                //############################################
                node: root + '/node/',
                getProperty: root + '/node/property',
                setProperty: root + '/node/property/save',
                galleryProcessor: root + '/imagegallery/',
                imageSets: root + '/imagegallery/imagesets/',
                galleryProcessorSave: root + '/imagegallery/save',
                imageSetsSave: root + '/imagegallery/imagesets/save'


            };

            /**
             * Set global variables (often used stuff)
             */
            $rootScope.initData = function () {
                $http.get($rootScope.REST.controllers).success(function (data) {
                    $rootScope.controllers = data;
                });


                $http.get($rootScope.REST.projectSettings).success(function (data) {
                    $rootScope.projectSettings = Essentials.keyValueAsDict(data.items);

                });

            };


            $rootScope.initData();
        })

        //############################################
        // DIRECTIVES
        //############################################
        .directive("messages", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    label: '@',
                    payload: '='
                },
                template: '<accordion close-others="oneAtATime"> \
                    <accordion-group is-open="isopen"> \
                    <accordion-heading ng-class="active"> \
                    {{label ? label : "Display changes made by this plugin"}}\
                    <i class="pull-right glyphicon" ng-class="{\'glyphicon-chevron-down\': isopen, \'glyphicon-chevron-right\': !isopen}"></i> \
                    </accordion-heading> \
                    <div ng-repeat="message in packageMessages.items" ng-switch="message.displayType"> \
                    <div  ng-switch-when="PRE"> \
                    <pre>{{message.value}}</pre> \
                    </div> \
                    <div ng-switch-when="A"> \
                    <a target="_blank" href="{{message.value}}">{{message.value}}</a> \
                    </div> \
                    <div ng-switch-when="H3"><h3 class="alert-info">{{message.value}}</h3> \
                    </div> \
                    <div ng-switch-when="H4"> \
                    <h3>{{message.value}}</h3> \
                    </div> \
                    <div ng-switch-when="STRONG"> \
                    <strong>{{message.value}}</strong></div> \
                    <div ng-switch-when="BR"><br/></div> \
                    <div ng-switch-default class="alert-info"><br/>{{message.value}}</div> \
                    </div></accordion-group> \
                    </accordion>',
                controller: function ($scope, installerFactory) {
                    // refresh messages when changes are made:
                    $scope.$watch('payload', function (newValue, oldValue) {
                        if (newValue) {
                            return installerFactory.packageMessages($scope.payload).success(function (data) {
                                return $scope.packageMessages = data;
                            });
                        }
                    }, true);
                    return installerFactory.packageMessages($scope.payload).success(function (data) {
                        return $scope.packageMessages = data;
                    });
                }
            }
        }).directive("templatePlugin", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    label: '@',
                    pluginId: '@',
                    pluginTitle: '@',
                    buttonText: '@',
                    sample: '@'
                },
                template: '<div class="panel-heading">{{pluginTitle}}</div> \
            <div class="panel-body"> \
                <form novalidate name="templateForm"> \
            <div class="form-group"> \
                <label>Please select templating language:</label> \
            </div> \
            <div class="radio"> \
                <label> \
                    <input type="radio" ng-model="templateName" required value="jsp"/> \
                    JSP template \
                </label> \
            </div> \
            <div class="radio"> \
                <label> \
                    <input type="radio" ng-model="templateName" required value="freemarker"/> \
                    Freemarker template \
                </label> \
            </div> \
            <div ng-show="sample" class="form-group"> \
                    <label> Content: </label>\
            </div> \
            <div ng-show="sample" class = "checkbox">\
                <label>  \
                Add sample content <input type="checkbox" ng-model="sampleData"/> \
                </label> \
                </div> \
            \
        </form> \
        <messages  payload="payload"/> \
         </div> \
        <div class="panel-footer">    \
            <div class = "input-group">\
                <span class="input-group-btn"> \
                    <button class="btn btn-primary pull-right" type="submit" ng-click="run()">{{buttonText}}</button>\
                </span> \
                </div> \
            </div>',
                controller: function ($scope, $sce, $log, $rootScope, $http) {
                    $scope.pluginId = "eventsPlugin";
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
                }

            }
        })
        .factory('installerFactory', function ($http, $rootScope) {
            var packageMessages = function (payload) {
                return $http.post($rootScope.REST.packageMessages, payload);
            };
            return {
                packageMessages: packageMessages
            };
        })


//############################################
// FILTERS
//############################################

    /**
     * Filter plugins for given group type
     */
        .filter('pluginType', function () {
            return function (name, plugins) {
                var retVal = [];
                angular.forEach(plugins, function (plugin) {
                    if (plugin.type == name) {
                        retVal.push(plugin);
                    }
                });
                return retVal;
            }
        }).filter('splitString', function () {
            return function (input, splitOn, idx) {
                if (input) {
                    var split = input.split(splitOn);
                    if (split.length >= idx) {
                        return split[idx];
                    }
                }
                return "";
            }
        })
        .filter('startsWith', function () {
            return function (inputCollection, inputString) {
                var collection = [];
                if (inputCollection && inputString) {
                    for (var i = 0; i < inputCollection.length; i++) {
                        if (inputCollection[i].value.slice(0, inputString.length) == inputString) {
                            collection.push(inputCollection[i]);
                        }
                    }
                    return collection;
                }
                return collection;
            }
        })

        //############################################
        // BROADCAST SERVICE
        //############################################
        .factory('eventBroadcastService', function ($rootScope) {
            var broadcaster = {};
            broadcaster.event = '';
            broadcaster.eventName = '';
            broadcaster.broadcast = function (eventName, event) {
                this.event = event;
                this.eventName = eventName;
                this.broadcastItem();
            };
            broadcaster.broadcastItem = function () {
                $rootScope.$broadcast(this.eventName);
            };
            return broadcaster;
        });

})();




