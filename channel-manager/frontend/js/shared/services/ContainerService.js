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

    angular.module('hippo.channelManager')

        .service('hippo.channelManager.Container', [
            '$log',
            '$rootScope',
            '$state',
            '_hippo.channelManager.IFrameService',
            '_hippo.channelManager.OutstandingHttpRequests',
            'hippo.channelManager.menuManager.FormValidationService',
            function($log, $rootScope, $state, IFrameService, OutstandingHttpRequests, FormValidationService) {

                function handleClose() {
                    if (IFrameService.isActive) {
                        var iframePanel = IFrameService.getContainer();

                        iframePanel.hostToIFrame.subscribe('close-request', function() {
                            if (OutstandingHttpRequests.isEmpty() && FormValidationService.getValidity()) {
                                $log.debug('close-reply-ok');
                                iframePanel.iframeToHost.publish('close-reply-ok');
                            } else if ($state.current.name == 'menu-item.add') {
                                $log.debug('show close confirmation dialog');

                                // show close confirmation dialog
                                $rootScope.$broadcast('close-confirmation:show');
                            } else {
                                $log.debug('close-reply-not-ok');
                                iframePanel.iframeToHost.publish('close-reply-not-ok');

                                // show close confirmation dialog
                                $rootScope.$broadcast('close-confirmation:show');
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