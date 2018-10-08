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

class TreeNodeCtrl {
  constructor($element, $scope) {
    'ngInject';

    this.$element = $element;
    this.$scope = $scope;
  }

  /**
   * Lookup for the tree component scope recursevly
   */
  _getTreeScope() {
    let treeScope = this.$scope;
    do {
      treeScope = treeScope.$parent;
    } while (treeScope && !treeScope.hippoTree);

    return treeScope;
  }

  /**
   * Get the external scope via a parent of transcluded scope
   */
  _getOuterScope() {
    const treeScope = this._getTreeScope();
    const transcludedScope = treeScope && treeScope.$parent;

    return transcludedScope && transcludedScope.$parent;
  }

  /**
   * Render component
   */
  _render() {
    if (!this.scope.hippoTree) {
      return;
    }

    this.scope.hippoTree.renderTreeTemplate(
      this.scope,
      dom => this.$element.replaceWith(dom),
    );
  }

  $onInit() {
    this.scope = this._getOuterScope()
      .$new();

    this.$scope.$watch(() => this.item, (item) => {
      this.scope.item = item;
    });

    this.$scope.$watch(() => this.uiTreeNode, (uiTreeNode) => {
      this.scope.toggle = uiTreeNode && uiTreeNode.scope.toggle;
    });

    this.$scope.$watch(() => this.hippoTree, (hippoTree) => {
      this.scope.hippoTree = hippoTree;
      this._render();
    });
  }
}

export default TreeNodeCtrl;
