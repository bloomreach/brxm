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

    .controller('hippo.channel.menu.TreeCtrl', [
      '$scope',
      '$state',
      '$stateParams',
      '$rootScope',
      '$log',
      'hippo.channel.ConfigService',
      'hippo.channel.FeedbackService',
      'hippo.channel.FormStateService',
      'hippo.channel.menu.MenuService',
      function ($scope, $state, $stateParams, $rootScope, $log, ConfigService, FeedbackService, FormStateService, MenuService) {
        var TreeCtrl = this;

        function setErrorFeedback (errorResponse) {
          $scope.MenuItemCtrl.feedback = FeedbackService.getFeedback(errorResponse);
        }

        function editItem (itemId) {
          $scope.MenuItemCtrl.feedback = '';

          $state.go('menu-item.edit', {
            menuItemId: itemId
          });
        }

        function selectItem (itemId) {
          if (FormStateService.isDirty()) {
            if (FormStateService.isValid()) {
              MenuService.saveMenuItem($scope.MenuItemCtrl.selectedMenuItem).then(function () {
                  editItem(itemId);
                },
                function (error) {
                  setErrorFeedback(error);
                  FormStateService.setValid(false);
                }
              );
            }
          } else {
            editItem(itemId);
          }
        }

        TreeCtrl.callbacks = {
          accept: function () {
            // created an issue for the Tree component, to add a disabled state
            // link: https://github.com/JimLiu/angular-ui-tree/issues/63
            // for now, simply don't accept any moves when the form is invalid
            return FormStateService.isValid();
          },
          dropped: function (event) {
            var source = event.source,
              sourceNodeScope = source.nodeScope,
              sourceId = sourceNodeScope.$modelValue.id,
              dest = event.dest,
              destNodesScope = dest.nodesScope,
              destId = destNodesScope.$nodeScope ? destNodesScope.$nodeScope.$modelValue.id : ConfigService.menuId;

            if (source.nodesScope !== destNodesScope || source.index !== dest.index) {
              MenuService.moveMenuItem(sourceId, destId, dest.index);
            }

            if($scope.MenuItemCtrl.selectedMenuItem.id !== sourceId) {
              selectItem(sourceId);
            }
          }
        };
      }
    ]);
}());
