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

app.run(function ($rootScope) {
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
        powerpacks_install: root + '/powerpacks/install/'

    }

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













