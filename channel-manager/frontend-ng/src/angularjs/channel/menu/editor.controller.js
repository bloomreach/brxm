/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class MenuEditorCtrl {
  constructor($scope, SiteMenuService, FormStateService) {
    'ngInject';

    this.SiteMenuService = SiteMenuService;
    this.FormStateService = FormStateService;

    SiteMenuService.getMenu(this.menuUuid)
      .then((menu) => {
        this.items = menu.items;
        this.selectedItem = this.items.length > 0 ? this.items[0] : undefined;

        $scope.$watch(
          () => menu.items,
          () => {
            this.items = menu.items;
            $scope.$broadcast('menu-items-changed');

            // merge pending changes into newly loaded tree
            if (this.items.length > 0 && this.selectedItem) {
              SiteMenuService.getMenuItem(this.menuUuid, this.selectedItem.id).then((item) => {
                if (this.selectedItem !== item) {
                  delete this.selectedItem.items;
                  this.selectedItem = angular.extend(item, this.selectedItem);
                }
              });
            }
          },
          false);
      })
      .catch(() => this.onError({ key: 'ERROR_MENU_LOAD_FAILED' }));

    this.treeOptions = {
      // created an issue for the Tree component, to add a disabled state
      // link: https://github.com/JimLiu/angular-ui-tree/issues/63
      // for now, simply don't accept any moves when the form is invalid
      accept: () => FormStateService.isValid(),
      dropped: (event) => {
        const source = event.source;
        const sourceNodeScope = source.nodeScope;
        const sourceId = sourceNodeScope.$modelValue.id;
        const dest = event.dest;
        const destNodesScope = dest.nodesScope;
        const destId = destNodesScope.$nodeScope ? destNodesScope.$nodeScope.$modelValue.id : this.menuUuid;

        if (source.nodesScope !== destNodesScope || source.index !== dest.index) {
          SiteMenuService.moveMenuItem(sourceId, destId, dest.index);
        }

        if (this.selectedItem.id !== sourceId) {
          this.selectItem(sourceId);
        }
      },
    };
  }

  selectItem(itemId) {
    if (this.FormStateService.isDirty()) {
      if (this.FormStateService.isValid()) {
        const saved = () => this.editItem(itemId);
        const failed = (error) => {
          this.onError({ key: 'ERROR_MENU_SAVE_FAILED', params: [error] });
          this.FormStateService.setValid(false);
        };
        this.SiteMenuService.saveMenuItem(this.selectedItem).then(saved, failed);
      }
    } else {
      this.editItem(itemId);
    }
  }

  editItem() {
  }
}
