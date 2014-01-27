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

    angular.module('hippo.channelManager.menuManagement')

        .service('hippo.channelManager.menuManagement.IFrameService', ['$window', function ($window) {

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
                    iframePanel = parent.Ext.getCmp(iframePanelId);

                if (parent.Ext.isEmpty(iframePanel)) {
                    throw new Error("Unknown iframe panel id: '" + iframePanelId + "'");
                }

                return iframePanel;
            }

            function getConfig() {
                var iframePanel = getParentIFramePanel(),
                    config = iframePanel.initialConfig.pageManagementConfig;

                if (config === undefined) {
                    throw new Error("Parent iframe panel does not contain page management config");
                }

                return config;
            }

            return {
                isActive: ($window.self !== $window.top),
                getConfig: getConfig,
                getContainer: getParentIFramePanel
            };
        }]);
})();