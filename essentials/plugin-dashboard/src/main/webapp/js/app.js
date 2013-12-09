'use strict';
/**
 * needed for dynamic loading of controllers
 * @type object {{$controllerProvider: $controllerProvider,
        $compileProvider: $compileProvider,
        $provide: $provide}}
 * @global
 */
var _PROVIDERS = {};

var app = angular.module('Essentials', ['ngRoute'], function ($controllerProvider, $compileProvider, $provide) {

    _PROVIDERS = {
        $controllerProvider: $controllerProvider,
        $compileProvider: $compileProvider,
        $provide: $provide
    };


});

//############################################
// GLOBAL LOADING
//############################################
app.config(function ($provide, $httpProvider) {
    $provide.factory('MyHttpInterceptor', function ($q, $rootScope, $log) {
        return {

            //############################################
            // REQUEST
            //############################################
            request: function (config) {
                $rootScope.busyLoading = true;
                return config || $q.when(config);
            },
            requestError: function (error) {
                $rootScope.busyLoading = true;
                $rootScope.globalError = [];
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
                $log.info(data);
                return data || $q.when(data);
            },
            responseError: function (error) {
                $rootScope.busyLoading = false;
                $rootScope.globalError = [];
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
});

//############################################
// RUN
//############################################


app.run(function ($rootScope,$log, $http, $templateCache, MyHttpInterceptor) {
    $rootScope.headerMessage = "Welcome on the Hippo Trail";
    $rootScope.packsInstalled = false;
    var root = 'http://localhost:8080/dashboard/rest';
    /* TODO generate this server side */
    $rootScope.REST = {
        root: root,
        menus: root + '/menus/',
        plugins: root + '/plugins/',
        status: root + '/status/',
        powerpacks: root + '/powerpacks/',
        beanwriter: root + '/beanwriter/',
        documentTypes: root + '/documenttypes/',
        controllers: root + '/controllers/',
        powerpacks_install: root + '/powerpacks/install/'

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
});

/*

define([
    'angular',
    'angular-route',
    'controllers',
    'routes'
], function (angular) {
    'use strict';


});
*/


