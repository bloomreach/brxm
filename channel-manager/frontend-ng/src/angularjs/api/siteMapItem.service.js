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

class SiteMapItemService {
  constructor($q, ConfigService, HstService, FeedbackService) {
    'ngInject';

    this.$q = $q;
    this.ConfigService = ConfigService;
    this.HstService = HstService;
    this.FeedbackService = FeedbackService;
  }

  loadAndCache(siteMapId, itemId) {
    this.clear();
    this._load(siteMapId, itemId)
      .then((item) => {
        this.item = item;
        this.siteMapId = siteMapId;
      })
      .catch(() => {
        this.FeedbackService.showError('ERROR_SITEMAP_ITEM_RETRIEVAL_FAILED');
      });
  }

  hasItem() {
    return !!this.item;
  }

  get() {
    return this.item;
  }

  clear() {
    delete this.item;
  }

  isLocked() {
    return this.hasItem() && angular.isString(this.item.lockedBy) && this.item.lockedBy !== this.ConfigService.cmsUser;
  }

  isEditable() {
    return this.hasItem() && !this.item.isHomePage && !this.isLocked() && this.item.workspaceConfiguration && !this.item.inherited;
  }

  deleteItem() {
    if (!this.hasItem()) {
      return this.$q.reject();
    }
    return this.HstService.doPost(null, this.siteMapId, 'delete', this.item.id);
  }

  updateItem(item, siteMapId) {
    return this.HstService.doPost(item, siteMapId, 'update')
      .then(response => response.data);
  }

  _load(siteMapId, itemId) {
    return this.HstService.doGet(siteMapId, 'item', itemId)
      .then(response => response.data);
  }
}

export default SiteMapItemService;
