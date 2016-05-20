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

export class SiteMenuService {
  constructor($q, HstService) {
    'ngInject';

    this.$q = $q;
    this.HstService = HstService;

    this.menu = {
      id: null,
      items: [],
    };
    this.loadMenuPromise = null;
  }

  getMenu(menuId) {
    return this._loadMenu(menuId);
  }

  getMenuItem(menuId, menuItemId) {
    return this._loadMenu(menuId).then((menu) => this._findMenuItem(menu.items, menuItemId));
  }

  _loadMenu(menuUuid) {
    if (this.loadMenuPromise === null) {
      this.loadMenuPromise = this.HstService.doGet(menuUuid)
        .then((response) => {
          if (response.data.items && !angular.equals(this.menu.items, response.data.items)) {
            // if response items are different, copy response items into menu
            angular.copy(response.data, this.menu);
            // collapse all nodes with childNodes
            this.addCollapsedProperties(this.menu.items, true);
          }
          this.menu.id = response.data.id || null;
          return this.menu;
        });
    }
    return this.loadMenuPromise;
  }

  addCollapsedProperties(items, collapsed) {
    if (angular.isArray(items)) {
      items.forEach((item) => {
        if (item.items && item.items.length > 0) {
          item.collapsed = collapsed;
          this.addCollapsedProperties(item.items, collapsed);
        }
      });
    }
  }

  _findMenuItem(items, id) {
    let found = null;

    if (angular.isArray(items)) {
      items.some((item) => {
        found = item.id === id ? item : this._findMenuItem(item.items, id);
        return found !== null;
      });
    }

    if (found !== null && found.linkType) {
      if (found.linkType === 'SITEMAPITEM') {
        found.sitemapLink = found.link;
      } else if (found.linkType === 'EXTERNAL') {
        found.externalLink = found.link;
      }
    }
    return found;
  }
}
