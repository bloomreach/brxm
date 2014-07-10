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
    angular.module('hippo.essentials', [ 'hippo.theme', 'ngSanitize', 'ngRoute','ngAnimate', 'localytics.directives', 'ui.bootstrap', 'ui.router'])

//############################################
// GLOBAL LOADING
//############################################
        .config(function ($provide, $httpProvider) {

            function addError($rootScope, error) {
                if(!error){
                    return;
                }
                if (error.data) {
                    if (error.data.value) {
                        $rootScope.feedbackMessages.push({type: 'error', message: error.data.value});
                    } else {
                        $rootScope.feedbackMessages.push({type: 'error', message: error.data});
                    }
                }
                else if (error.status) {
                    $rootScope.feedbackMessages.push({type: 'error', message: error.status});
                } else {
                    $rootScope.feedbackMessages.push({type: 'error', message: error});
                }

            }

            function addMessage($rootScope, data) {
                if(!data){
                    return;
                }
                if (data.data && data.data.successMessage) {
                    $rootScope.feedbackMessages.push({type: 'info', message: data.data.value});
                } else if (data.successMessage) {
                    $rootScope.feedbackMessages.push({type: 'info', message: data.successMessage.value});
                }

            }

            $provide.factory('MyHttpInterceptor', function ($q, $rootScope, $log) {
                return {
                    //############################################
                    // REQUEST
                    //############################################
                    request: function (config) {
                        return config || $q.when(config);
                    },
                    requestError: function (error) {
                        $rootScope.busyLoading = true;
                        addError($rootScope, error);
                        return $q.reject(error);
                    },

                    //############################################
                    // RESPONSE
                    //############################################
                    response: function (data) {
                        $rootScope.busyLoading = false;
                        // show success message:
                        addMessage($rootScope, data);
                        return data || $q.when(data);
                    },
                    responseError: function (error) {
                        $rootScope.busyLoading = false;
                        addError($rootScope, error);

                        return $q.reject(error);
                    }
                };
            });
            $httpProvider.interceptors.push('MyHttpInterceptor');
        })

//############################################
// RUN
//############################################


        .run(function ($rootScope, $location, $log, $http, $timeout, modalService) {
            $rootScope.feedbackMessages = [];
            $rootScope.headerMessage = "Welcome on the Hippo Trail";
            $rootScope.applicationUrl = 'http://localhost:8080/essentials';
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
                ping: plugins + '/ping/',
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

                package_install: plugins + '/install/package',
                save_settings: plugins + '/savesettings',
                project_settings: plugins + '/projectsettings',

                //############################################
                // NODE
                //############################################
                node: root + '/node/',
                getProperty: root + '/node/property',
                setProperty: root + '/node/property/save',

                //############################################
                // DOCUMENTS
                //############################################
                documents: root + '/documents/',
                documents_compounds: root + '/documents/' + 'compounds',
                documents_documents: root + '/documents/' + 'documents',
                documents_template_queries: root + '/documents/' + 'templatequeries'


            };

            /**
             * Set global variables (often used stuff)
             */
            $rootScope.initData = function () {

                var PING_RUNNING_TIMER = 7000;
                var PING_DOWN_TIMER = 10000;
                //############################################
                // PINGER
                //############################################
                // keep reference to old modal:
                var pingModal = null;
                (function ping() {

                    var modalOptions = {
                        closeButtonText: '',
                        actionButtonText: 'Close',
                        headerText: 'Service Down',
                        bodyText: 'The Essentials dashboard server appears to be down. If you are' +
                            ' rebuilding the project, please wait until it is up and running again.'

                    };

                    function openModal() {
                        if (pingModal == null) {
                            pingModal = modalService.showModal({}, modalOptions);
                            pingModal.then(function () {
                                // discard modal
                                pingModal = null;
                            });
                        }
                    }

                    $http.get($rootScope.REST.ping).success(function (data) {
                        if (data === 'true') {
                            $timeout(ping, PING_RUNNING_TIMER);
                        } else {
                            // app is back up, but needs to restart
                            window.location.href = $rootScope.applicationUrl;
                        }
                    }).error(function () {
                        openModal();
                        $timeout(ping, PING_DOWN_TIMER);
                    });

                })();

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
        // FACTORIES
        //############################################

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
        })
        .service('modalService', function ($modal) {
            /**
             *
             * NOTE: template must be here because if server is down,
             * template cannot be serverd anymore
             */
            var modalDefaults = {
                backdrop: true,
                keyboard: true,
                modalFade: true,
                template: '<div class="modal-header"><h3>{{modalOptions.headerText}}</h3></div>' +
                    '<div class="modal-body"><p>{{modalOptions.bodyText}}</p></div>' +
                    '<div class="modal-footer">' +
                    '<button type="button" ng-hide="!modalOptions.closeButtonText" class="btn" data-ng-click="modalOptions.close()">{{modalOptions.closeButtonText}}</button>' +
                    '<button ng-hide="!modalOptions.actionButtonText" class="btn btn-primary" data-ng-click="modalOptions.ok();">{{modalOptions.actionButtonText}}</button>' +
                    '</div>'

            };
            var modalOptions = {
                closeButtonText: "",
                actionButtonText: "",
                headerText: "",
                bodyText: ""
            };

            this.showModal = function (customModalDefaults, customModalOptions) {
                if (!customModalDefaults) {
                    customModalDefaults = {};
                }
                customModalDefaults.backdrop = 'static';
                return this.show(customModalDefaults, customModalOptions);

            };

            this.show = function (customModalDefaults, customModalOptions) {
                var myDefaults = {};
                var myOptions = {};
                angular.extend(myDefaults, modalDefaults, customModalDefaults);
                angular.extend(myOptions, modalOptions, customModalOptions);

                if (!myDefaults.controller) {
                    myDefaults.controller = function ($scope, $modalInstance) {
                        $scope.modalOptions = myOptions;
                        $scope.modalOptions.ok = function (result) {
                            $modalInstance.close(result);
                        };
                        $scope.modalOptions.close = function (result) {
                            $modalInstance.dismiss('cancel');
                        };
                        myOptions = $scope.modalOptions;
                    }
                }
                var myResult = $modal.open(myDefaults).result;
                myResult.options = myOptions;
                return  myResult;
            };
        }
    );

})();




