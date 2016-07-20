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
  constructor($scope, $translate, SiteMenuService, HippoIframeService, DialogService,
              FeedbackService, ChannelService, PickerService, ConfigService) {
    'ngInject';

    this.$translate = $translate;
    this.SiteMenuService = SiteMenuService;
    this.HippoIframeService = HippoIframeService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.ChannelService = ChannelService;
    this.PickerService = PickerService;
    this.ConfigService = ConfigService;

    this.isSaving = {};
    this.isDragging = false;
    this.errorMap = {
      ITEM_ALREADY_LOCKED: 'ERROR_MENU_LOCKED_BY',
      ITEM_NAME_NOT_UNIQUE: 'ERROR_MENU_SAME_NAME_SIBLING',
      ITEM_NAME_NOT_UNIQUE_IN_ROOT: 'ERROR_MENU_SAME_NAME_SIBLING',
    };

    this._loadMenu()
      .then((menu) => {
        if (this.isLockedByOther()) {
          this.FeedbackService.showErrorOnSubpage('ERROR_MENU_LOCKED_BY', { lockedBy: this.lockedBy });
        }
        // Currently, the SiteMenuService is loading and maintaining the menu structure.
        // Creation or deletion of a menu item trigger a full reload of the menu, and the
        // $watch below makes sure the MenuEditorCtrl becomes aware of these reloads.
        // TODO: this is ugly, inefficient and hard to maintain. We should improve this.
        $scope.$watch(
          () => menu.items,
          () => {
            this.items = menu.items;
          }
        );
      })
      .catch((response) => {
        response = response || {};

        HippoIframeService.reload(); // Make sure EditMenu buttons are up-to-date.
        this.onError({ key: 'ERROR_MENU_LOAD_FAILED', params: response.data });
      });

    this.treeOptions = {
      dragStart: () => {
        this.isDragging = true;
      },
      dragStop: () => {
        this.isDragging = false;
      },
      dropped: (event) => {
        const source = event.source;
        const sourceNodeScope = source.nodeScope;
        const sourceId = sourceNodeScope.$modelValue.id;
        const dest = event.dest;
        const destNodesScope = dest.nodesScope;
        const destId = destNodesScope.$nodeScope ? destNodesScope.$nodeScope.$modelValue.id : undefined;

        if (source.nodesScope !== destNodesScope || source.index !== dest.index) {
          SiteMenuService.moveMenuItem(sourceId, destId, dest.index)
            .then(() => { this.isMenuModified = true; })
            .catch((response) => this._handleError(response, 'ERROR_MENU_MOVE_FAILED'));
        }
      },
    };
  }

  _loadMenu() {
    return this.SiteMenuService.loadMenu(this.menuUuid)
      .then((menu) => {
        this.lockedBy = menu.lockedBy;
        this.items = menu.items;
        return menu;
      });
  }

  _startEditingItem(item) {
    this.editingItem = item;
  }

  isLockedByOther() {
    return this.lockedBy && this.lockedBy !== this.ConfigService.cmsUser;
  }

  stopEditingItem() {
    this.editingItem = null;
  }

  toggleEditState(item) {
    if (!this.editingItem || this.editingItem.id !== item.id) {
      this.SiteMenuService.getEditableMenuItem(item.id)
        .then((editableItem) => {
          this.isAdding = false;
          this._startEditingItem(editableItem);
        });
    } else {
      this.stopEditingItem();
    }
  }

  addItem() {
    this.isSaving.newItem = true;
    this.SiteMenuService.createEditableMenuItem(this.selectedItem)
      .then((editableItem) => {
        this.isMenuModified = true;
        this.isAdding = true;
        this.selectedItem = editableItem;
        this._startEditingItem(editableItem);
      })
      .catch((response) => this._handleError(response, 'ERROR_MENU_CREATE_FAILED'))
      .finally(() => delete this.isSaving.newItem);
  }

  showPicker(targetEvent) {
    const pickerTypes = [
      {
        id: this.SiteMenuService.getSiteContentIdentifier(),
        name: this.$translate.instant('PICKER_TYPE_DOCUMENTS'),
        type: 'documents',
      },
      {
        id: this.SiteMenuService.getSiteMapIdentifier(),
        name: this.$translate.instant('PICKER_TYPE_SITEMAP_ITEMS'),
        type: 'pages',
      },
    ];
    const pickerCfg = {
      locals: {
        pickerTypes,
        initialLink: this.editingItem.sitemapLink,
      },
      targetEvent,
    };

    this.PickerService.show(pickerCfg).then(({ pathInfo }) => {
      this.editingItem.sitemapLink = this.editingItem.link = pathInfo;
    });
  }

  onBack() {
    if (this.isMenuModified) {
      this.HippoIframeService.reload();
      this.ChannelService.recordOwnChange();
    }
    this.onDone();
  }

  saveItem() {
    this.SiteMenuService.saveMenuItem(this.editingItem)
      .then(() => {
        this.isMenuModified = true;
        this.stopEditingItem();
      })
      .catch((response) => this._handleError(response, 'ERROR_MENU_ITEM_SAVE_FAILED'));
  }

  _handleError(response, defaultKey) {
    this.FeedbackService.showErrorResponseOnSubpage(response, defaultKey, this.errorMap);

    if (response && response.errorCode === 'ITEM_ALREADY_LOCKED') {
      this._loadMenu();
      this.ChannelService.reload(); // pull in recent 'changedBySet' for Change Management
    }
  }

  _doDelete() {
    return this.SiteMenuService.deleteMenuItem(this.editingItem.id)
      .then(() => {
        this.isMenuModified = true;
        this.stopEditingItem();
      })
      .catch((response) => this._handleError(response, 'ERROR_MENU_ITEM_DELETE_FAILED'));
  }

  _confirmDelete() {
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_MENU_ITEM_MESSAGE', {
        menuItem: this.editingItem.title,
      }))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  deleteItem() {
    this._confirmDelete().then(() => this._doDelete());
  }

  hasLocalParameters() {
    return Object.keys(this.editingItem.localParameters).length !== 0;
  }
}
