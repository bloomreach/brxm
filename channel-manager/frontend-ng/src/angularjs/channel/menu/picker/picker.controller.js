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
    this.$filter = $filter;
    this.PickerService = PickerService;

    this.items = PickerService.getTree();
    this.selectedItem = null;
    this.selectedDocument = null;

    this.pickerType = this.pickerTypes[0];
    this.PickerService.loadDataForLink(this.pickerTypes[0].id, this.initialLink)
      .then((pickerType) => {
        this.pickerType = this.pickerTypes.find((pt) => pt.type === pickerType);
        this._navigateToSelectedOrRoot();
      });

    this.treeOptions = {
      displayItem: (item) => item.type === 'folder' || item.type === 'page',
      selectItem: (item) => {
        PickerService.getData(item);
        this.selectedDocument = item.selectable ? item : null;
      },
      toggleItem: (item) => {
        PickerService.getData(item);
      },
    };
  }

  changePickerType() {
    this.PickerService.loadDataForLink(this.pickerType.id)
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
    return items.some((item) => {
      if (item.selected) {
        this.selectedItem = parent;
        this.selectedDocument = item;
        return true;
      }
      if (item.items) {
        return this._navigateToSelected(item.items, item);
      }
      return false;
    });
  }
}
