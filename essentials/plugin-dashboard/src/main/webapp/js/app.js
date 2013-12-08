/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

'use strict';

var app = angular.module('Essentials', ['ngRoute']);
app.run(function ($rootScope) {
    $rootScope.packsInstalled = false;
    var root = 'http://localhost:8080/dashboard/rest';
    /* TODO generate this server side */
    $rootScope.REST = {
        root: root,
        menus: root + '/menus',
        plugins: root + '/plugins',
        status: root + '/status'

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
                $rootScope.globalError.push(error.data);
                return $q.reject(error);
            },

            //############################################
            // RESPONSE
            //############################################
            response: function (data) {
                $rootScope.busyLoading = false;
                $rootScope.globalError = false;
                $log.info(data);
                return data || $q.when(data);
            },
            responseError: function (error) {
                $rootScope.busyLoading = false;
                $rootScope.globalError = true;
                $rootScope.globalError.push(error.data);
                $log.error(error);
                return $q.reject(error);
            }
        };
    });
    $httpProvider.interceptors.push('MyHttpInterceptor');
});













