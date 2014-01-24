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

    angular.module('hippo.essentials', ['ngRoute', 'localytics.directives'])

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
                    if(!$rootScope.FEEDBACK_TIMER){
                        $rootScope.FEEDBACK_TIMER = new Date();
                    }
                    $rootScope.busyLoading = true;
                    var date = new Date();
                    // keep success messages for 5 seconds
                    if((date.getTime() - $rootScope.FEEDBACK_TIMER.getTime())> 5000){
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
                    if(data.data.successMessage){
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


    .run(function ($rootScope, $location, $log, $http, $templateCache, MyHttpInterceptor) {
        $rootScope.headerMessage = "Welcome on the Hippo Trail";
        // routing listener
        $rootScope.$on('$routeChangeStart', function (event, next, current) {
            // check if we need powerpacks install check
            /*if(!$rootScope.checkDone && ($location.url() != "/" || $location.url() != "")){
                var url = $location.url();
                $log.info("Redirecting to [/]: needs powerpack install check:");
                $location.path('/');
            }*/

        });



        var root = 'http://localhost:8080/essentials/rest';
        var plugins = root + "/plugins";
        /* TODO generate this server side */
        $rootScope.REST = {
            root: root,
            menus: root + '/menus/',

            /**
             * Returns list of all plugins
             * //TODO: change this once we have marketplace up and running
             */
            plugins: root +"/plugins/",
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

            status: root + '/status/',
            powerpacks: root + '/powerpacks/',
            beanwriter: root + '/beanwriter/',
            documentTypes: root + '/documenttypes/',
            controllers: root + '/controllers/',
            powerpacks_install: root + '/powerpacks/install/'  ,

            compounds: root + '/documenttypes/compounds',
            compoundsCreate: root + '/documenttypes/compounds/create/' ,
            compoundsDelete: root + '/documenttypes/compounds/delete/' ,
            contentblocksCreate: root + '/documenttypes/compounds/contentblocks/create/',
            //############################################
            // NODE
            //############################################
            node: root + '/node/',
            getProperty: root + '/node/property',
            setProperty: root + '/node/property/save'


        };

        $rootScope.initData = function () {
            $http({
                method: 'GET',
                url: $rootScope.REST.controllers
            }).success(function (data) {
                        $rootScope.controllers = data;
                        // load all controller files:
                        /*var controller = $rootScope.controllers.controller;
                         for(var i = 0; i < controller.length; i++){
                         $log.info(controller[i].id);
                         require(["plugins/"+ controller[i].id+"/controller.js"]);
                         $log.info("Loaded: " + controller[i].id);
                         }*/

                    });

        };



        $rootScope.initData();
    })

//############################################
// FILTERS
//############################################

    .filter('splitString', function () {
        return function (input, splitOn, idx) {
            if(input){
                var split = input.split(splitOn);
                if(split.length >= idx){
                    return split[idx];
                }
            }
            return "";
        }})
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
    });
})();




