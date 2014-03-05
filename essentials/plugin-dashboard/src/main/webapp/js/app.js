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

    angular.module('hippo.essentials', ['ngRoute', 'localytics.directives', 'ui.bootstrap', 'ui.sortable', 'ui.router'])

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

                    // check if we need powerpacks install check
                    /*  if(!$rootScope.checkDone && (toState != "powerpacks" && $location.url() != "/" || $location.url() != "")){
                     $state.transitionTo('powerpacks');
                     }*/

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
                    powerpacksStatus: plugins + '/status/powerpack/',
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


                    documentTypes: root + '/documenttypes/',

                    powerpacks_install: plugins + '/install/powerpack',

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
            .directive("powerpacks", function () {
                return {
                    replace: true,
                    restrict: 'E',
                    scope: {
                        label: '@',
                        options: '=',
                        selectedDescription: '=',
                        ngModel: '=',
                        onSelect: '&'
                    },

                    link: function (scope, element, attrs, ctrl) {
                        scope.onPowerpackSelect = function () {
                            scope.onSelect();
                        }
                    },

                    template: '<div><select  ng-required="true" ng-selected="onPowerpackSelect()" ng-model="ngModel">' +
                            ' <option ng-repeat="option in options" value="{{option.pluginId}}" ng-disabled="!option.enabled">{{option.name}}</option>' +
                            '</select>' +
                            '<div class="clearfix sep-10">&nbsp;</div>' +
                            '<div id="option.description">{{selectedDescription}}</div>' +
                            '<div class="clearfix sep-10">&nbsp;</div>' +
                            '</div>'
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




