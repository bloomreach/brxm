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

    angular.module('hippo.channelManager.menuManager', ['ngRoute', 'hippo.theme', 'pascalprecht.translate', 'ui.router'])

        .config(['$stateProvider', '$translateProvider', function($stateProvider, $translateProvider) {

            // routing
            $stateProvider
                .state('loader', {
                    url: '/loader',
                    controller: 'hippo.channelManager.menuManager.LoaderCtrl',
                    templateUrl: 'states/loader/loader.html'
                })

                .state('menu-item', {
                    abstract: true,
                    controller: 'hippo.channelManager.menuManager.MenuItemCtrl',
                    templateUrl: 'states/menu-item/menu-item.html'
                })

                .state('menu-item.add', {
                    url: '/:menuItemId/add',
                    controller: 'hippo.channelManager.menuManager.AddMenuItemCtrl',
                    templateUrl: 'states/menu-item/add/add-menu-item.html'
                })

                .state('menu-item.add-page', {
                    url: '/:menuItemId/add',
                    controller: 'hippo.channelManager.menuManager.AddPageCtrl',
                    templateUrl: 'states/menu-item/add-page/add-page.html'
                })

                .state('menu-item.edit', {
                    url: '/:menuItemId/edit',
                    controller: 'hippo.channelManager.menuManager.EditMenuItemCtrl',
                    templateUrl: 'states/menu-item/edit/edit-menu-item.html'
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
            '$state',
            'hippo.channelManager.menuManager.Container',
            'hippo.channelManager.menuManager.ConfigService',
            '_hippo.channelManager.menuManagement.IFrameService',
            function ($rootScope, $translate, $state, Container, Config, IFrame) {
                $rootScope.close = function() {
                    Container.close();
                };

                // go to default state
                $state.go('loader');

                // set language
                $translate.uses(Config.locale);

                // enable live reload
                IFrame.enableLiveReload();
            }
        ]);
}());