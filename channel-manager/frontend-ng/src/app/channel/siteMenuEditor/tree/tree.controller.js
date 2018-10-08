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

class HippoTreeCtrl {
  constructor($scope, $filter, $transclude) {
    'ngInject';

    this.$scope = $scope;
    this.getByPropertyFilter = $filter('getByProperty');
    this.renderTreeTemplate = $transclude;

    $scope.$watch(() => this.items, (newItems, oldItems) => {
      const oldNodes = this._collectNodes(oldItems, {});
      const newNodes = this._collectNodes(newItems, {});
      this._copyCollapsedState(oldNodes, newNodes);
    });
  }

  toggle(item) {
    item.collapsed = !item.collapsed;

    // when collapsing an item with a selected (grand)child, select the item itself
    if (this.selectedItem && this.selectable !== false && item.collapsed && this.getByPropertyFilter(item.items, 'id', this.selectedItem.id, 'items')) {
      this.selectItem(item);
    }

    if (angular.isFunction(this.options.toggleItem)) {
      this.options.toggleItem(item);
    }
  }

  selectItem(item) {
    this.selectedItem = item;

    if (angular.isFunction(this.options.selectItem)) {
      this.options.selectItem(item);
    }
  }

  displayItem(item) {
    if (angular.isFunction(this.options.displayItem)) {
      return this.options.displayItem(item);
    }
    return true;
  }

  isEmpty(item) {
    return !item.items.length;
  }

  _collectNodes(items, map) {
    angular.forEach(items, (item) => {
      map[item.id] = item;
      this._collectNodes(item.items, map);
    });
    return map;
  }

  _copyCollapsedState(srcNodes, targetNodes) {
    angular.forEach(srcNodes, (srcNode) => {
      const targetNode = targetNodes[srcNode.id];
      if (targetNode) {
        targetNode.collapsed = srcNode.collapsed;
      }
    });
  }
}


export default HippoTreeCtrl;
