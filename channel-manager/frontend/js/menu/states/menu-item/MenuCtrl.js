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

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.MenuCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            'hippo.channel.menu.MenuService',
            function ($scope, $state, $stateParams, MenuService) {
                $scope.addItem = function () {
                    // handle the split between sitemap and external links. Grab whichever
                    // one is used and save that as link and then destroy them both
                    var savedMenuItem = angular.copy($scope.$parent.selectedMenuItem);

                    if (savedMenuItem.linkType === 'SITEMAPITEM') {
                        savedMenuItem.link = savedMenuItem.sitemapLink;
                    } else if (savedMenuItem.linkType === 'EXTERNAL') {
                        savedMenuItem.link = savedMenuItem.externalLink;
                    } else if (savedMenuItem.linkType === 'NONE') {
                        delete savedMenuItem.link;
                    }
                    delete savedMenuItem.sitemapLink;
                    delete savedMenuItem.externalLink;

                    // only navigate to the add menu item form when there are no
                    // validation errors for the edit menu item form
                    MenuService.saveMenuItem(savedMenuItem).then(function () {
                            savedMenuItem.collapsed = false;
                            // navigate to the add menu item state
                            var parentItemId = $stateParams.menuItemId || $stateParams.menuId;
                            $state.go('menu-item.add', {menuItemId: parentItemId});
                        },
                        function (errorResponse) {}
                    );
                };
            }
        ]);
}());
