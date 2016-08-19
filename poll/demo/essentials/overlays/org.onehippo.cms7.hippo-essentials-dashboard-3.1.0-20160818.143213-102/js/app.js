/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
 * do not include dependencies like below, but add them to this (app.js) module). An example:
 * <pre>
 (function () {
    "use strict";
    angular.module('hippo.essentials')
    })();
 * </pre>
 *
 * Note that dependencies are missing (['ngRoute', etc.)
 *
 */
(function () {
    "use strict";
    angular.module('hippo.essentials', [ 'hippo.theme', 'ngSanitize', 'ui.bootstrap', 'ui.router'])

//############################################
// GLOBAL LOADING
//############################################
        .config(function ($provide, $httpProvider) {

            function addError($rootScope, error) {
                if (!error) {
                    return;
                }
                // avoid error messages if pinging fails
                if (error.config && error.config.url) {
                    var url = error.config.url;
                    if (url.substring(url.length - 5, url.length) === '/ping') {
                        return;
                    }
                }
                if (error.data) {
                    if (error.data.value) {
                        $rootScope.feedbackMessages.push({type: 'error', message: error.data.value});
                    } else {
                        $rootScope.feedbackMessages.push({type: 'error', message: error.data});
                    }
                } else if (error.status) {
                    $rootScope.feedbackMessages.push({type: 'error', message: error.status});
                } else {
                    $rootScope.feedbackMessages.push({type: 'error', message: error});
                }
            }

            function addMessage($rootScope, data) {
                if (!data) {
                    return;
                }
                if (data.data && data.data.items) {
                    var items = data.data.items;
                    angular.forEach(items, function (v) {
                        if (v.successMessage && v.globalMessage) {
                            $rootScope.feedbackMessages.push({type: 'info', message: v.value});
                        }
                    });
                }
                else if (data.data && data.data.successMessage && data.data.globalMessage) {
                    $rootScope.feedbackMessages.push({type: 'info', message: data.data.value});
                } else if (data.successMessage && data.globalMessage) {
                    $rootScope.feedbackMessages.push({type: 'info', message: data.successMessage.value});
                }

            }

            $provide.factory('MyHttpInterceptor', function ($q, $rootScope) {
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

            // IE caches GET requests. Below configuration bypasses this to make Essentials work in IE.
            if (!$httpProvider.defaults.headers.get) {
                $httpProvider.defaults.headers.get = {};
            }
            $httpProvider.defaults.headers.get['If-Modified-Since'] = 'Mon, 26 Jul 1997 05:00:00 GMT';
            $httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
        })

//############################################
// RUN
//############################################
        .run(function ($rootScope, $location, $log, $http, $templateCache) {
            $rootScope.$on('$stateChangeStart',
                function (event, toState, toParams, fromState, fromParams) {
                    // when showing introduction, we want to hide some items:
                    $rootScope.INTRODUCTION_DISPLAYED = false;
                    if (toState && toState.url) {
                        // disable caching of pages:
                        $templateCache.remove(toState.url);
                        if (toState.templateUrl){
                            $templateCache.remove(toState.templateUrl);
                        }

                        // if introduction, hide menu:
                        if (toState.url.indexOf('/introduction') != -1) {
                            $rootScope.INTRODUCTION_DISPLAYED = true;
                        }
                        if (toState.url.indexOf('/tools') != -1) {
                            $rootScope.mainHeader = 'Tools';
                        }
                        else if (toState.url.indexOf('/build') != -1) {
                            $rootScope.mainHeader = 'Build instructions';
                        }
                        else if (toState.url.indexOf('/library') != -1) {
                            $rootScope.mainHeader = 'Library';
                        }
                        else if (toState.url.indexOf('/installed-features') != -1) {
                            $rootScope.mainHeader = 'Installed features';
                        } else {
                            $rootScope.mainHeader = 'Hippo Essentials';
                        }
                    } else {
                        $rootScope.mainHeader = 'Hippo Essentials';
                    }
                });

            $rootScope.feedbackMessages = [];
            $rootScope.headerMessage = "Welcome on the Hippo Trail";
            $rootScope.applicationUrl = 'http://' + window.SERVER_URL + '/essentials';
            var root = 'http://' + window.SERVER_URL + '/essentials/rest';
            var pluginsStem = root + "/plugins";
            var projectStem = root + "/project";

            /* TODO generate this server side ?*/
            $rootScope.REST = {
                root: root,
                menus: root + '/menus/',
                jcr: root + '/jcr/',
                jcrQuery: root + '/jcr/query/',
                dynamic: root + '/dynamic/',

                /**
                 * PluginResource
                 */
                plugins: pluginsStem,
                PLUGINS: { // Front-end API
                    byId:               function(id) { return pluginsStem + '/' + id; },
                    changesById:        function(id) { return pluginsStem + '/' + id + '/changes'; },
                    setupById:          function(id) { return pluginsStem + '/' + id + '/setup'; },
                    setupCompleteForId: function(id) { return pluginsStem + '/' + id + '/setupcomplete'; }
                },

                /**
                 * ProjectResource
                 */
                project: projectStem,
                PROJECT: { // Front-end API
                    settings:    projectStem + '/settings',
                    coordinates: projectStem + '/coordinates'
                },

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
                $http.get($rootScope.REST.PROJECT.settings).success(function (data) {
                    $rootScope.projectSettings = Essentials.keyValueAsDict(data.items);
                });
            };

            $rootScope.initData();
        })


//############################################
// FILTERS
//############################################

    /**
     * Filter plugins for given group type
     */
        .filter('pluginType', function () {
            return function (plugins, name) {
                var retVal = [];
                angular.forEach(plugins, function (plugin) {
                    if (plugin.type == name) {
                        retVal.push(plugin);
                    }
                });
                return retVal;
            };
        })

        .service('modalService', function ($uibModal) {
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
                    '<div class="modal-body" ng-bind-html="modalOptions.bodyText"></div>' +
                    '<div ng-hide="!modalOptions.closeButtonText && !modalOptions.actionButtonText" class="modal-footer">' +
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
                    myDefaults.controller = function ($scope, $uibModalInstance) {
                        $scope.modalOptions = myOptions;
                        $scope.modalOptions.ok = function (result) {
                            $uibModalInstance.close(result);
                        };
                        $scope.modalOptions.close = function () {
                            $uibModalInstance.dismiss('cancel');
                        };
                        myOptions = $scope.modalOptions;
                    };
                }
                var myResult = $uibModal.open(myDefaults).result;
                myResult.options = myOptions;
                return  myResult;
            };
        }
    );

})();




