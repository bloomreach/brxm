/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
    .controller('hippo.channel.menu.PickerCtrl', [
      '$state',
      '$stateParams',
      '$filter',
      'hippo.channel.menu.PickerService',
      function ($state, $stateParams, $filter, PickerService) {
        var PickerCtrl = this;

        function navigateToSelected (items, parent) {
          angular.forEach(items, function (item) {
            if (item.selected) {
              PickerCtrl.selectedItem = parent;
              PickerCtrl.selectedDocument = item;
              $state.go('picker.docs', {
                pickerTreeItemId: PickerCtrl.selectedItem.id
              });
            }
            if (item.items) {
              navigateToSelected(item.items, item);
            }
          });
        }

        PickerCtrl.selectedItem = {};
        PickerCtrl.selectedDocument = null;
        PickerCtrl.treeItems = PickerService.getTree();
        PickerCtrl.pickerTypes = [
          {
            name: 'Documents',
            type: 'documents'
          },
          {
            name: 'Pages',
            type: 'pages'
          }
        ];

        PickerCtrl.selectDocument = function () {
          $state.go('menu-item.edit', {
            menuItemId: $stateParams.menuItemId,
            selectedDocumentPath: PickerCtrl.selectedDocument.pathInfo
          });
        };
        PickerCtrl.cancelPicker = function () {
          $state.go('menu-item.edit', {
            menuItemId: $stateParams.menuItemId
          });
        };
        PickerCtrl.changePickerType = function () {
          $state.go('picker', {
            menuItemId: $stateParams.menuItemId,
            siteContentIdentifier: $stateParams.siteContentIdentifier,
            siteMapIdentifier: $stateParams.siteMapIdentifier,
            link: $stateParams.link
          });
          if (PickerCtrl.pickerType.name == 'Pages') {
            PickerService.getInitialData($stateParams.siteMapIdentifier).then(function () {
              navigateToSelected(PickerCtrl.treeItems);
            });
          } else {
            $stateParams.link = null;
            PickerService.getInitialData($stateParams.siteContentIdentifier, $stateParams.link).then(function () {
              navigateToSelected(PickerCtrl.treeItems);
            });
          }
          PickerCtrl.selectedDocument = null;
        };

        PickerCtrl.pickerType = $filter('hippoGetByProperty')(PickerCtrl.pickerTypes, 'type', PickerCtrl.treeItems[0].pickerType);
        navigateToSelected(PickerCtrl.treeItems);
        if(Object.getOwnPropertyNames(PickerCtrl.selectedItem).length === 0)
        {
          PickerCtrl.selectedItem = PickerCtrl.treeItems[0];
          $state.go('picker.docs', {
            pickerTreeItemId: PickerCtrl.selectedItem.id
          });

        }
      }
    ]);
}());
