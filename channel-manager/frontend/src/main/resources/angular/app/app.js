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
(function() {
    "use strict";

    function isInIFrame($window) {
        return $window.self !== $window.top;
    }

    function getParentIFramePanelId($window) {
        var idParam = 'parentExtIFramePanelId',
            search = $window.location.search,
            keyValue = search.split('=');
        if (keyValue[0] === ('?' + idParam)) {
            return keyValue[1];
        }
        throw new Error("Expected query parameter '" + idParam + "'");
    }

    function getParentIFramePanel($window) {
        var iframePanelId = getParentIFramePanelId($window),
            iframePanel = parent.Ext.getCmp(iframePanelId);

        if (parent.Ext.isEmpty(iframePanel)) {
            throw new Error("Unknown iframe panel id: '" + iframePanelId + "'");
        }

        return iframePanel;
    }

    function readConfigFromIFrame($window) {
        var iframePanel = getParentIFramePanel($window),
            config = iframePanel.initialConfig.pageManagementConfig;

        if (config === undefined) {
            throw new Error("Parent iframe panel does not contain page management config");
        }

        return config;
    }

    angular.module('hippo.channelManager.pageManagementApp', ['ngRoute'])
        .config(['$routeProvider',
            function($routeProvider) {
                $routeProvider
                    .when('/', {
                        controller: 'hippo.channelManager.main',
                        templateUrl: 'app/views/main.html'
                    })
                    .otherwise({
                        redirectTo: '/'
                    });
            }
        ])
        .service('hippo.channelManager.pageManagementConfig', ['$window',
            function($window) {
                var config;

                if (isInIFrame($window)) {
                    config = readConfigFromIFrame($window);
                } else {
                    config = {
                        menuId: 'hardcodedMenuId',
                        apiUrlPrefix: 'hardcodedApiUrlPrefix'
                    };
                }

                return config;
            }
        ])
        .service('hippo.channelManager.Container', ['$window', '$location',
            function($window, $location) {
                var iframePanel;

                if (isInIFrame($window)) {
                    iframePanel = getParentIFramePanel($window);
                }

                return {

                    close: function() {
                        if (iframePanel) {
                            iframePanel.iframeToHost.publish('close');
                        } else {
                            console.info("Ignoring close, there is no parent iframe");
                        }
                    }

                };
            }
        ])
        .controller('hippo.channelManager.main', ['hippo.channelManager.pageManagementConfig', 'hippo.channelManager.Container',
            function(Config, Container) {
                this.items = [ 'one', 'two', 'three' ];

                this.close = function() {
                    Container.close();
                };
            }
        ]);

}());