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
  constructor($q, $filter, $scope, $translate, SiteMenuService, FormStateService, HippoIframeService, DialogService) {
    'ngInject';

    this.$q = $q;
    this.$filter = $filter;
    this.$translate = $translate;
    this.SiteMenuService = SiteMenuService;
    this.FormStateService = FormStateService;
    this.HippoIframeService = HippoIframeService;
    this.DialogService = DialogService;

    this.isSaving = {};

    SiteMenuService.loadMenu(this.menuUuid)
      .then((menu) => {
        this.items = menu.items;
        this.selectedItem = this.items.length > 0 ? this.items[0] : undefined;

        $scope.$watch(
          () => menu.items,
          () => {
            this.items = menu.items;
            $scope.$broadcast('menu-items-changed');
          }
        );

        $scope.$watch(
          () => this.selectedItem,
          (current, last) => {
            if (current && last && current.id !== last.id) {
              this._saveIfDirty(last);
            }
          }
        );
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
          SiteMenuService.moveMenuItem(this.menuUuid, sourceId, destId, dest.index)
            .catch(() => this.onError({ key: 'ERROR_MENU_MOVE_FAILED' }));
        }

        if (this.selectedItem.id !== sourceId) {
          this._saveIfDirty(this.selectedItem).then(() => this.selectItem(sourceId));
        }
      },
    };
  }

  selectItem(itemId) {
    if (!this.selectedItem || this.selectedItem.id !== itemId) {
      this.SiteMenuService.getMenuItem(this.menuUuid, itemId)
        .then((item) => {
          this.selectedItem = item;
        });
    }
  }

  stopEditingItem() {
    this.editingItem = null;
  }

  setupItem(item) {
    if (item.linkType === 'SITEMAPITEM') {
      item.sitemapLink = item.link;
    } else if (item.linkType === 'EXTERNAL') {
      item.externalLink = item.link;
    }
    this.editingItem = item;
  }

  editItem(item) {
    if (!this.editingItem || this.editingItem.id !== item.id) {
      this.SiteMenuService.getMenuItem(this.menuUuid, item.id)
        .then((retrievedItem) => {
          this.setupItem(retrievedItem);
        });
    } else {
      this.stopEditingItem();
    }
  }

  addItem() {
    this._saveIfDirty(this.selectedItem).then(() => {
      if (!this.selectedItem) {
        return;
      }

      this.isSaving.newItem = true;

      this.SiteMenuService.getMenu(this.menuUuid)
        .then((menu) => this._createBlankMenuItem(menu))
        .then((blankItem) => this.SiteMenuService.createMenuItem(this.menuUuid, blankItem, this.selectedItem.id))
        .then((newItem) => {
          this.FormStateService.setValid(true);
          this.isSaving.newItem = false;
          this.selectedItem = newItem;
        }).catch((error) => {
          this.isSaving.newItem = false;
          this.onError({ key: 'ERROR_MENU_CREATE_FAILED', params: [error] });
        });
    });
  }

  onBack() {
    this.HippoIframeService.reload().then(this.onDone);
  }

  _saveIfDirty(item) {
    if (this.FormStateService.isDirty()) {
      if (!this.FormStateService.isValid()) {
        return this.$q.reject();
      }
      const defer = this.$q.defer();
      this.SiteMenuService.saveMenuItem(item).then(
        () => defer.resolve(),
        (error) => {
          this.onError({ key: 'ERROR_MENU_SAVE_FAILED', params: [error] });
          this.FormStateService.setValid(false);
          defer.reject();
        });
      return defer.promise;
    }
    return this.$q.when();
  }

  _createBlankMenuItem(menu) {
    const incFilter = this.$filter('incrementProperty');
    const result = {
      linkType: 'SITEMAPITEM',
      title: incFilter(menu.items, 'title', this.$translate.instant('SUBPAGE_MENU_EDITOR_NEW_ITEM_TITLE'), 'items'),
      link: '',
    };
    if (angular.isObject(menu.prototypeItem)) {
      result.localParameters = angular.copy(menu.prototypeItem.localParameters);
    }
    return result;
  }

  saveItem(item) {
    this.SiteMenuService.saveMenuItem(this.menuUuid, item).then(() => {
      this.stopEditingItem(item);
    }).catch(() => {
      this.onError({ key: 'ERROR_MENU_ITEM_SAVE_FAILED' });
    });
  }

  _doDelete(item) {
    return this.SiteMenuService.deleteMenuItem(this.menuUuid, item.id).then(() => {
      this.stopEditingItem(item);
    }).catch(() => {
      this.onError({ key: 'ERROR_MENU_ITEM_DELETE_FAILED' });
    });
  }

  _confirmDelete(item) {
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_MENU_ITEM_MESSAGE', {
        menuItem: item.title,
      }))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  deleteItem(item) {
    this._confirmDelete(item).then(() => this._doDelete(item));
  }
}
