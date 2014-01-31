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

    var LIVE_RELOAD_URL = '//localhost:35729/livereload.js';

    angular.module('hippo.channelManager.menuManagement')

        .service('_hippo.channelManager.menuManagement.IFrameService', ['$window', '$log', function ($window, $log) {

            function getParentIFramePanelId() {
                var idParam = 'parentExtIFramePanelId',
                    search = $window.location.search,
                    keyValue = search.split('=');

                if (keyValue[0] === ('?' + idParam)) {
                    return keyValue[1];
                }

                throw new Error("Expected query parameter '" + idParam + "'");
            }

            function getParentIFramePanel() {
                var iframePanelId = getParentIFramePanelId($window),
                    iframePanel = $window.parent.Ext.getCmp(iframePanelId);

                if (!angular.isObject(iframePanel)) {
                    throw new Error("Unknown iframe panel id: '" + iframePanelId + "'");
                }

                return iframePanel;
            }

            function getConfig() {
                var iframePanel = getParentIFramePanel(),
                    config = iframePanel.initialConfig.iframeConfig;

                if (config === undefined) {
                    throw new Error("Parent iframe panel does not contain iframe configuration");
                }

                return config;
            }

            function addScriptToHead(scriptUrl) {
                var head = $window.document.getElementsByTagName("head")[0],
                    script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = scriptUrl;
                head.appendChild(script);
            }

            function enableLiveReload() {
                if (getConfig().debug) {
                    addScriptToHead(LIVE_RELOAD_URL);
                    $log.info("iframe #" + getParentIFramePanelId() + " has live reload enabled via " + LIVE_RELOAD_URL);
                }
            }

            return {
                isActive: ($window.self !== $window.top),
                getConfig: getConfig,
                getContainer: getParentIFramePanel,
                enableLiveReload: enableLiveReload
            };
        }]);
}());