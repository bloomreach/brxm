/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    "use strict";

    angular.module('hippo.channel')
        .factory('hippo.channel.HstApiRequests', [
            'hippo.channel.ConfigService',
            '$q', '$templateCache', function (ConfigService, $q, $templateCache) {
                return {
                    'request': function(config) {
                        if ($templateCache.get(config.url)) {
                            return config;
                        }
                        config.params = config.params || {};
                        if (config.url.indexOf(ConfigService.apiUrlPrefix) === 0) {
                            // Calling HST endpoints requires this GET parameter to be set
                            config.params.FORCE_CLIENT_HOST = true;
                            config.params.antiCache = new Date().getTime();
                        } else if (config.method === 'GET') {
                            config.params.antiCache = ConfigService.antiCache;
                        }
                        return $q.when(config);
                    }
                };
            }
        ])

        .config(['$httpProvider', function($httpProvider) {
            $httpProvider.interceptors.push('hippo.channel.HstApiRequests');
        }]);

}());
