/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

    .controller('hippo.channel.menu.LoaderCtrl', [
      '$log',
      '$state',
      'hippo.channel.menu.MenuService',
      function ($log, $state, MenuService) {
        MenuService.getMenu().then(
          function (menuData) {
            if (menuData.items && menuData.items.length > 0) {
              $state.go('menu-item.edit', {
                menuItemId: menuData.items[0].id
              });
            } else {
              $state.go('menu-item.none');
            }
          },
          function (error) {
            // TODO: show error in UI
            $log.error(error);
          }
        );
      }
    ]);
}());