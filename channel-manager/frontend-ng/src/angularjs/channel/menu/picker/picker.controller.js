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

export class PickerCtrl {
  constructor($mdDialog, $filter, PickerService) {
    'ngInject';

    if (!this.pickerTypes || this.pickerTypes.length === 0) {
      throw new Error('No types defined for picker');
    }

    this.$mdDialog = $mdDialog;
    this.PickerService = PickerService;
    this.$filter = $filter;

    this.items = PickerService.getTree();
    this.selectedItem = null;
    this.selectedDocument = null;

    this.pickerType = this.pickerTypes[0];
    this.PickerService.getInitialData(this.pickerTypes[0].id, this.initialLink)
      .then(() => this._navigateToSelectedOrRoot());

    this.treeOptions = {
      displayItem: (item) => item.type === 'folder' || item.type === 'page',
      selectItem: (item) => {
        if (!item.leaf && item.items.length === 0) {
          PickerService.getData(item);
        }
        this.selectedDocument = item.selectable ? item : null;
      },
      toggleItem: (item) => {
        if (item.collapsed === false && item.items.length === 0) {
          PickerService.getData(item);
        }
      },
    };
  }

  changePickerType() {
    this.PickerService.getInitialData(this.pickerType.id)
      .then(() => {
        const root = this.items[0];
        if (this.selectedItem !== null && this.selectedItem.type !== root.type) {
          // reset selected state when switching picker types
          this.selectedItem = null;
          this.selectedDocument = null;
        }
        this._navigateToSelectedOrRoot();
      });
  }

  cancel() {
    this.$mdDialog.cancel();
  }

  ok() {
    this.$mdDialog.hide(this.selectedDocument);
  }

  _navigateToSelectedOrRoot() {
    if (this.items && this.items.length) {
      this._navigateToSelected(this.items);
      if (this.selectedItem === null) {
        this.selectedItem = this.items[0];
      }
    }
  }
  _navigateToSelected(items, parent) {
    angular.forEach(items, (item) => {
      if (item.selected) {
        this.selectedItem = parent;
        this.selectedDocument = item;
        // TODO: break out?
      }
      if (item.items) {
        this._navigateToSelected(item.items, item);
      }
    });
  }
}
