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

    angular.module('hippo.channelManager.menuManager')

        .factory('_hippo.channelManager.menuManager.OutstandingHttpRequests', function() {
            var outstandingRequests = 0;

            return {
                request: function (config) {
                    outstandingRequests += 1;
                    return config;
                },

                response: function (response) {
                    outstandingRequests -= 1;
                    return response;
                },

                responseError: function (rejection) {
                    outstandingRequests -= 1;
                    return rejection;
                },

                isEmpty: function () {
                    return outstandingRequests === 0;
                }
            };
        })

        .config(['$httpProvider', function($httpProvider) {
            $httpProvider.interceptors.push('_hippo.channelManager.menuManager.OutstandingHttpRequests');
        }]);

}());

