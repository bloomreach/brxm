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

const FIRST_CHILD = 'first';
const NEXT_SIBLING = 'after';

class SiteMenuService {
  constructor($filter, $translate, HstService) {
    'ngInject';

    this.$filter = $filter;
    this.$translate = $translate;
    this.HstService = HstService;

    this.menu = {
      id: null,
      items: [],
    };
    this.loadMenuPromise = null;
  }

  loadMenu(menuId) {
    this.loadMenuPromise = null;
    this.menu.id = menuId;
    return this._loadMenu(menuId);
  }

  getEditableMenuItem(menuItemId) {
    return this._loadMenu(this.menu.id).then(() => this._makeEditableItem(menuItemId));
  }

  moveMenuItem(menuItemId, parentId, position) {
    const menuId = this.menu.id;
    parentId = parentId || menuId;
    return this.HstService.doPutWithHeaders(menuId, { 'Move-From': menuItemId }, parentId, String(position));
  }

  deleteMenuItem(menuItemId) {
    return this.HstService.doDelete(this.menu.id, menuItemId)
      .then(() => this.loadMenu(this.menu.id));
  }

  saveMenuItem(editableMenuItem) {
    // We copy the editable menu item "back" such that we don't lose state in case the saving fails.
    const menuItem = angular.copy(editableMenuItem);

    this._removeCollapsedProperties(menuItem);
    this._extractLinkFromSitemapLinkOrExternalLink(menuItem);

    return this.HstService.doPost(menuItem, this.menu.id)
      .then(() => {
        this._replaceMenuItem(this.menu.items, menuItem);
      });
  }

  getSiteContentIdentifier() {
    return this.menu.siteContentIdentifier;
  }

  getSiteMapIdentifier() {
    return this.menu.siteMapIdentifier;
  }

  /**
   * Create a new menu item.
   */
  createEditableMenuItem(markerItem) {
    const markerId = markerItem ? markerItem.id : null;
    const menuId = this.menu.id;

    return this._loadMenu(menuId)
      .then(() => {
        let parentId = menuId;
        const options = { position: NEXT_SIBLING };
        const newItem = this._createBlankMenuItem();
        const paths = this._findPathToMenuItem(this.menu, markerId);

        if (paths && paths.length) {
          let parentItem = paths.pop();
          if (parentItem.items && parentItem.items.length) {
            // if the parent has children, add the new node as first child
            parentId = parentItem.id;
            options.position = FIRST_CHILD;
            // and ensure the parent is not collapsed
            parentItem.collapsed = false;
          } else if (paths.length >= 1) {
            // if the parent has no children (yet), add the new node as next sibling of parent
            options.sibling = parentItem.id;
            parentItem = paths.pop();
            parentId = parentItem.id;
          }
        }

        return this.HstService.doPostWithParams(newItem, menuId, options, parentId)
          .then(response => response.data)
          .then(newItemId => this.loadMenu(menuId).then(() => this._makeEditableItem(newItemId)));
      });
  }

  getPathToMenuItem(menuId, menuItemId) {
    return this._loadMenu(menuId).then(menu => this._findPathToMenuItem(menu, menuItemId));
  }

  _createBlankMenuItem() {
    const incFilter = this.$filter('incrementProperty');
    const result = {
      linkType: 'NONE',
      title: incFilter(this.menu.items, 'title', this.$translate.instant('NEW_MENU_ITEM_TITLE'), 'items'),
    };
    if (angular.isObject(this.menu.prototypeItem)) {
      result.localParameters = angular.copy(this.menu.prototypeItem.localParameters);
    }
    return result;
  }

  _makeEditableItem(id) {
    let item = this._findMenuItem(this.menu.items, id);
    if (item) {
      item = angular.copy(item);
      this._setSitemapLinkOrExternalLinkFromLink(item);
    }
    return item;
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
    if (!angular.isArray(items)) {
      return null;
    }

    let found = null;
    items.some((item) => {
      found = item.id === id ? item : this._findMenuItem(item.items, id);
      return found !== null;
    });
    return found;
  }

  // lookup the existing menu item by ID and replace it with the updated item
  _replaceMenuItem(items, item) {
    if (angular.isArray(items)) {
      for (let i = 0; i < items.length; i += 1) {
        if (items[i].id === item.id) {
          items[i] = item;
          return true;
        }
        if (this._replaceMenuItem(items[i].items, item)) {
          return true;
        }
      }
    }
    return false;
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


export default SiteMenuService;
