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

    angular.module('hippo.channelManager.menuManagement', ['ngRoute', 'hippo.theme', 'pascalprecht.translate'])

        .config(['$routeProvider', '$translateProvider', function($routeProvider, $translateProvider) {

            // routing
            $routeProvider
                .when('/loader', {
                    controller: 'hippo.channelManager.menuManagement.LoaderCtrl',
                    templateUrl: 'views/loader.html'
                })
                .when('/add-menu-item', {
                    controller: 'hippo.channelManager.menuManagement.AddMenuItemCtrl',
                    templateUrl: 'views/add-menu-item.html'
                })
                .when('/:menuItemId/edit', {
                    controller: 'hippo.channelManager.menuManagement.EditMenuItemCtrl',
                    templateUrl: 'views/edit-menu-item.html'
                })
                .when('/:menuItemId/add-page', {
                    controller: 'hippo.channelManager.menuManagement.AddPageCtrl',
                    templateUrl: 'views/add-page.html'
                })
                .otherwise({
                    redirectTo: '/loader'
                });

            // translations
            $translateProvider.useStaticFilesLoader({
                prefix: 'i18n/',
                suffix: '.json'
            });

            $translateProvider.preferredLanguage('en');
        }])

        .run([
            '$rootScope',
            '$translate',
            'hippo.channelManager.menuManagement.Container',
            'hippo.channelManager.menuManagement.ConfigService',
            '_hippo.channelManager.menuManagement.IFrameService',
            function ($rootScope, $translate, Container, Config, IFrame) {
                $rootScope.close = function() {
                    Container.close();
                };

                // set language
                $translate.uses(Config.locale);

                IFrame.enableLiveReload();
            }
        ]);
}());