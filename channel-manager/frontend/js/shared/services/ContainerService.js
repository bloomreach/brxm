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

    angular.module('hippo.channel')

        .service('hippo.channel.Container', [
            '$log',
            '$rootScope',
            '_hippo.channel.IFrameService',
            '_hippo.channel.OutstandingHttpRequests',
            'hippo.channel.FormValidationService',
            function($log, $rootScope, IFrameService, OutstandingHttpRequests, FormValidationService) {

                function handleClose() {
                    if (IFrameService.isActive) {
                        var iframePanel = IFrameService.getContainer();

                        iframePanel.hostToIFrame.subscribe('close-request', function() {
                            var event = $rootScope.$broadcast('before-close');
                            if (!event.defaultPrevented) {
                                iframePanel.iframeToHost.publish('close-reply-ok');
                            } else {
                                // show close confirmation dialog
                                $rootScope.$broadcast('close-confirmation:show');
                            }
                        });
                        $rootScope.$on('before-close', function(event) {
                            if (!OutstandingHttpRequests.isEmpty() || !FormValidationService.getValidity()) {
                                event.preventDefault();
                            }
                        });
                    }
                }

                function performClose() {
                    var iFramePanel = IFrameService.getContainer();
                    iFramePanel.iframeToHost.publish('close-reply-ok');
                }

                return {
                    handleClose: handleClose,
                    performClose: performClose
                };
            }
        ]);
}());