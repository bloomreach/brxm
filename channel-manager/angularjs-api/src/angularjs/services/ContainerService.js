/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

    function prefixWithSlash(str) {
        if (str === '' || str.charAt(0) !== '/') {
            return '/' + str;
        }
        return str;
    }

    angular.module('hippo.channel')

        .service('hippo.channel.Container', [
            '$log',
            '$rootScope',
            '_hippo.channel.IFrameService',
            function($log, $rootScope, IFrameService) {

                function close() {
                    IFrameService.publish('close');
                }

                function showPage(path, mountId) {
                    IFrameService.publish('browseTo', prefixWithSlash(path), mountId);
                }

                return {
                    close: close,
                    showPage: showPage
                };
            }
        ]);
}());
