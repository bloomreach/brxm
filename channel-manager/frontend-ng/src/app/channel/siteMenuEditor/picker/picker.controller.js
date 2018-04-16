/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import './picker.scss';

class PickerCtrl {
  constructor($mdDialog, $q, PickerService, locals) {
    'ngInject';

    this.$mdDialog = $mdDialog;
    this.$q = $q;
    this.PickerService = PickerService;
    this.pickerTypes = locals.pickerTypes;
    this.initialLink = locals.initialLink;

    if (!this.pickerTypes || this.pickerTypes.length === 0) {
      throw new Error('No types defined for picker');
    }

    this.items = this.PickerService.getTree();
    this.selectedItem = null;
    this.selectedDocument = null;

    if (!this.initialLink) {
      this._loadInitialPicker();
    } else {
      this._loadSelectedPicker(this.pickerTypes[0].id, this.initialLink)
        .then(itemFound => (itemFound ? true : this._loadSelectedPicker(this.pickerTypes[1].id, this.initialLink)))
        .then(itemFound => (itemFound ? true : this._loadInitialPicker()));
    }

    this.treeOptions = {
      displayItem: item => item.type === 'folder' || item.type === 'page',
      selectItem: (item) => {
        this.PickerService.getData(item);
        this.selectedDocument = item.selectable ? item : null;
      },
      toggleItem: (item) => {
        this.PickerService.getData(item);
      },
    };
  }

  _loadInitialPicker() {
    this.PickerService.loadDataForLink(this.pickerTypes[0].id)
      .then((pickerType) => {
        this.pickerType = this.pickerTypes.find(pt => pt.type === pickerType);
        this._navigateToRoot();
      });
  }

  _loadSelectedPicker(pickerTypeId, link) {
    return this.PickerService.loadDataForLink(pickerTypeId, link)
      .then((pickerType) => {
        this.pickerType = this.pickerTypes.find(pt => pt.type === pickerType);
        return this._navigateToSelected(this.items);
      });
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

  _navigateToRoot() {
    this.selectedItem = this.items && this.items.length ? this.items[0] : null;
  }

  _navigateToSelected(items, parent) {
    if (!items || items.length === 0) {
      return false;
    }

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

  _navigateToSelectedOrRoot() {
    if (!this._navigateToSelected(this.items)) {
      this._navigateToRoot();
    }
  }
}

export default PickerCtrl;
