/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
    angular.module('hippo.essentials', [ 'hippo.theme', 'ngAnimate', 'ngSanitize', 'ui.bootstrap', 'ui.router'])

//############################################
// GLOBAL LOADING
//############################################
        .config(function ($provide, $httpProvider, $locationProvider) {

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
                if (error.data && error.data.feedbackMessages) {
                    addFeedbackMessages($rootScope, error.data.feedbackMessages);
                } else if (error.data) {
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

            function addMessage($rootScope, response) {
                if (response && response.data && response.data.feedbackMessages) {
                    addFeedbackMessages($rootScope, response.data.feedbackMessages);
                }
            }

            function addFeedbackMessages($rootScope, messages) {
                angular.forEach(messages, function (details) {
                    $rootScope.feedbackMessages.push({
                        type: details.error ? 'error' : 'info',
                        message: details.message
                    });
                });
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

            $locationProvider.hashPrefix('');
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
            $rootScope.applicationUrl = window.SERVER_URL + '/essentials';

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
        })
        .service('pluginService', function($http, $q, $log) {
            var baseUrl = window.SERVER_URL + '/essentials/rest/plugins';
            var changeMessageTypeMap = {
                FILE_CREATE: {
                    sort: 1,
                    icon: 'file'
                },
                FILE_DELETE: {
                    sort: 1,
                    icon: 'file'
                },
                DOCUMENT_REGISTER: {
                    sort: 2,
                    icon: 'file-text'
                },
                XML_NODE_CREATE: {
                    sort: 3,
                    icon: 'plus-square'
                },
                XML_NODE_DELETE: {
                    sort: 3,
                    icon: 'plus-square'
                },
                XML_NODE_FOLDER_CREATE: {
                    sort: 4,
                    icon: 'folder-open'
                },
                EXECUTE: {
                    sort: 5,
                    icon: 'code'
                }
            };
            var plugins = [];
            function loadPlugins() {
                return $http.get(baseUrl).then(function(response) {
                    if (plugins.length !== response.data.length) {
                        // a new number of plugins has been returned, replace the old list.
                        plugins.splice(0, plugins.length);
                        response.data.forEach(function(plugin) {
                            plugins.push(plugin);
                        });
                    } else {
                        // to avoid a jumpy UX, only update the plugins' installState
                        response.data.forEach(function(incoming) {
                            plugins.some(function(existing) {
                                if (existing.id === incoming.id) {
                                    existing.installState = incoming.installState;
                                }
                            });
                        });
                    }
                });
            }
            this.loadPlugins = loadPlugins;
            this.getPlugins = function() {
                return plugins;
            };
            this.getPlugin = function(id) {
                return plugins.find(function(plugin) {
                    return plugin.id === id;
                });
            };
            this.install = function(id, params) {
                var plugin = this.getPlugin(id);
                var postedParams = (plugin.installState === 'awaitingUserInput') ? params : {};
                return $http.post(baseUrl + '/' + id + '/install', postedParams).then(loadPlugins);
            };
            this.autoSetup = function() {
                return $http.post(baseUrl + '/autosetup').then(loadPlugins, loadPlugins);
            };
            this.getChangeMessages = function(id, params) {
                return $http.get(baseUrl + '/' + id + '/changes', {params: params}).then(function (response) {
                    var messages = response.data || [];
                    messages.forEach(function(message) {
                        message.icon = changeMessageTypeMap[message.type].icon;
                    });
                    messages.sort(function(m1, m2) {
                        return changeMessageTypeMap[m1.type].sort - changeMessageTypeMap[m2.type].sort;
                    });
                    return messages;
                });
            };
        })
        .service('projectService', function($http, $q) {
            var baseUrl = window.SERVER_URL + '/essentials/rest/project';
            var _settings;
            function loadSettings() {
                return $http.get(baseUrl + '/settings').then(function(response) {
                    _settings = response.data;
                });
            }
            this.getSettings = function() {
                if (_settings) {
                    return $q.resolve(_settings);
                }
                return loadSettings().then(function() {
                    return _settings;
                });
            };
            this.setSettings = function(settings) {
                _settings = undefined;
                return $http.post(baseUrl + '/settings', settings).then(loadSettings);
            };
            this.ping = function() {
                return $http.get(baseUrl + '/ping');
            };
            this.status = function() {
                return $http.get(baseUrl + '/status');
            };
        });
})();




