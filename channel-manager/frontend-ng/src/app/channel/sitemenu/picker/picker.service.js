/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

import template from './picker.html';

class PickerService {
  constructor(DialogService, HstService) {
    'ngInject';

    this.DialogService = DialogService;
    this.HstService = HstService;
    this.treeData = { items: [] };
  }

  show(cfg) {
    return this.DialogService.show(angular.extend(cfg, {
      clickOutsideToClose: true,
      template,
      controller: 'PickerCtrl',
      controllerAs: 'picker',
      bindToController: true,
    }));
  }

  getTree() {
    return this.treeData.items;
  }

  loadDataForLink(id, link) {
    return this.HstService.doGet(id, 'picker', link)
      .then((response) => {
        this.treeData.items.splice(0, this.treeData.items.length);
        this.treeData.items[0] = response.data;
        return response.data.pickerType;
      });
  }

  getData(item) {
    return this.HstService.doGet(item.id, 'picker').then((response) => {
      item.items = response.data.items;
    });
  }
}

export default PickerService;
