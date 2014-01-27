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

    angular.module('hippo.channelManager.menuManagement', ['ngRoute', 'hippo.theme'])

        .config(['$routeProvider', function($routeProvider) {
                $routeProvider
                    .when('/loader', {
                        controller: 'hippo.channelManager.menuManagement.LoaderCtrl',
                        templateUrl: 'app/views/loader.html'
                    })
                    .when('/:menuItemId/edit', {
                        controller: 'hippo.channelManager.menuManagement.EditMenuItemCtrl',
                        templateUrl: 'app/views/edit-menu-item.html'
                    })
                    .when('/:menuItemId/add-page', {
                        controller: 'hippo.channelManager.menuManagement.AddPageCtrl',
                        templateUrl: 'app/views/add-page.html'
                    })
                    .otherwise({
                        redirectTo: '/loader'
                    });
            }
        ])

        .run([
            '$rootScope',
            'hippo.channelManager.menuManagement.Container',
            function ($rootScope, Container) {
                $rootScope.close = function() {
                    Container.close();
                };
            }
        ]);
}());