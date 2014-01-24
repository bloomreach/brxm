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

    angular.module('hippo.channelManager.pageManagementApp', ['ngRoute', 'hippo.theme'])
        .config(['$routeProvider',
            function($routeProvider) {
                $routeProvider
                    .when('/', {
                        controller: 'hippo.channelManager.main',
                        templateUrl: 'app/views/edit-menu-item.html'
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
        ])

        .controller('TreeController', ['$scope', '$http', function($scope, $http) {
            $scope.panelVisible = false;

            // pages
            // edit: 1
            // add:  2
            $scope.page = 1;

            $http.get('site/_rp/fd694c53-5be6-4690-b83e-7b78cf7a2268')

            $scope.exampleTreedata = [{
                label: 'Home',
                destination: 1,
                sitemapItem: '/home'
            }, {
                label: 'News & Events',
                destination: 1,
                sitemapItem: '/home'
            }, {
                label: 'Jobs',
                destination: 1,
                sitemapItem: '/home'
            }, {
                label: 'Products',
                destination: 1,
                sitemapItem: '/home'
            }, {
                label: 'About',
                destination: 1,
                sitemapItem: '/home'
            }, {
                label: 'Other items',
                children: [
                    'PHP',
                    'JavaScript', {
                        label: 'Other-languages',
                        children: [
                            'PHP',
                            'JavaScript',
                            'ActionScript 3.0'
                        ]
                    }, {
                        label: 'Download',
                        destination: 2,
                        externalLink: 'http://www.github.com'
                    }, {
                        label: '42'
                    },
                ]
            }];

            $scope.selectedItem = $scope.exampleTreedata[0];
            $scope.viewItem = function (item) {
                console.log(item);
                $scope.selectedItem = item;
            };

            $scope.addPage = function () {
                console.log('Click add page');
                $scope.page = 2;
            };

            $scope.templates = [{
                name: 'Blogpost',
                id: 1
            }, {
                name: 'Newsitem',
                id: 2
            }, {
                name: 'Products',
                id: 3
            }];

            $scope.template = { 'multiple': false };

            $scope.createPage = function (newpage) {
                $scope.selectedItem.
                    $scope.page = 1;
                return newpage;
            };

            $scope.deleteItem = function (item) {
                var index = $scope.exampleTreedata.indexOf(item);
                $scope.exampleTreedata.splice(index, 1);
                $scope.selectedItem = $scope.exampleTreedata[$scope.exampleTreedata.length - 1];
            };

            $scope.addMenuItem = function () {
                $scope.exampleTreedata.forEach(function (item) {
                    item.selected = false;
                });

                $scope.exampleTreedata.push({
                    label: 'New menu item',
                    destination: 'false',
                    selected: true
                });

                $scope.selectedItem = $scope.exampleTreedata[$scope.exampleTreedata.length - 1];
            };

            $scope.cancel = function () {
                $scope.page = 1;
            };

            $scope.closeContainer = function () {
                $scope.panelVisible = false;
            };


            // edit channel
            $scope.editChannelVisible = true;
            $scope.editChannel = function () {
                $scope.editMenuVisible = true;
            };

            // edit menu
            $scope.editMenu = function () {
                $scope.panelVisible = true;
            };

            $scope.editMenuVisible = false;
        }]);

}());