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

const NEXT_SIBLING = 'after';

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
    if (menuId !== this.menu.id) {
      this.loadMenuPromise = null;
    }
    return this._loadMenu(menuId);
  }

  loadMenu(menuId) {
    this.loadMenuPromise = null;
    return this._loadMenu(menuId);
  }

  getMenuItem(menuId, menuItemId) {
    return this._loadMenu(menuId).then((menu) => this._findMenuItem(menu.items, menuItemId));
  }

  moveMenuItem(menuId, menuItemId, parentId, position) {
    return this.HstService.doPost({}, menuId, 'move', menuItemId, parentId, String(position));
  }

  deleteMenuItem(menuId, menuItemId) {
    return this.HstService.doPost({}, menuId, 'delete', menuItemId)
      .then(() => this.loadMenu(menuId));
  }

  saveMenuItem(menuId, menuItem) {
    const menuItemCopy = angular.copy(menuItem);

    // TODO: needed?
    this._removeCollapsedProperties(menuItemCopy);
    this._extractLinkFromSitemapLinkOrExternalLink(menuItemCopy);
    return this.HstService.doPost(menuItemCopy, menuId);
  }

  /**
   * Create a new menu item.

   * @param menuId The menu id
   * @param newItem The item to be created
   * @returns {promise|Promise.promise|Q.promise}
   */
  createMenuItem(menuId, newItem) {
    const options = {
      position: NEXT_SIBLING,
    };
    const lastItem = this.menu.items[this.menu.items.length - 1];
    if (lastItem) {
      options.sibling = lastItem;
    }
    const parentId = menuId;

    return this.HstService.doPostWithParams(newItem, menuId, options, 'create', parentId)
      .then((response) => response.data)
      .then((newItemId) => this.loadMenu(menuId).then((menu) => this._findMenuItem(menu.items, newItemId)));
  }

  getPathToMenuItem(menuId, menuItemId) {
    return this._loadMenu(menuId).then((menu) => this._findPathToMenuItem(menu, menuItemId));
  }

  _loadMenu(menuId) {
    if (this.loadMenuPromise === null) {
      this.loadMenuPromise = this.HstService.doGet(menuId)
        .then((response) => {
          if (response.data.items && !angular.equals(this.menu.items, response.data.items)) {
            // if response items are different, copy response items into menu
            angular.copy(response.data, this.menu);
            // collapse all nodes with childNodes
            this._addCollapsedProperties(this.menu.items, true);
          }
          this.menu.id = response.data.id || null;
          return this.menu;
        });
    }
    return this.loadMenuPromise;
  }

  _addCollapsedProperties(items, collapsed) {
    if (angular.isArray(items)) {
      items.forEach((item) => {
        if (item.items && item.items.length > 0) {
          item.collapsed = collapsed;
          this._addCollapsedProperties(item.items, collapsed);
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

    if (found !== null) {
      this._setSitemapLinkOrExternalLinkFromLink(found);
    }
    return found;
  }

  _findPathToMenuItem(parent, id) {
    let found = null;

    parent.items.every((item) => {
      if (item.id === id) {
        found = [item];
      } else if (item.items) {
        found = this._findPathToMenuItem(item, id);
      }
      return !found;
    });

    if (found) {
      found.unshift(parent);
    }

    return found;
  }

  _setSitemapLinkOrExternalLinkFromLink(item) {
    if (item.linkType) {
      if (item.linkType === 'SITEMAPITEM') {
        item.sitemapLink = item.link;
      } else if (item.linkType === 'EXTERNAL') {
        item.externalLink = item.link;
      }
    }
  }

  _extractLinkFromSitemapLinkOrExternalLink(item) {
    if (item.linkType === 'SITEMAPITEM') {
      item.link = item.sitemapLink;
    } else if (item.linkType === 'EXTERNAL') {
      item.link = item.externalLink;
    } else if (item.linkType === 'NONE') {
      delete item.link;
    }
    delete item.sitemapLink;
    delete item.externalLink;

    angular.forEach(item.items, (subItem) => {
      this._extractLinkFromSitemapLinkOrExternalLink(subItem);
    });
  }

  _removeCollapsedProperties(item) {
    delete item.collapsed;
    angular.forEach(item.items, (subItem) => {
      this._removeCollapsedProperties(subItem);
    });
  }
}

