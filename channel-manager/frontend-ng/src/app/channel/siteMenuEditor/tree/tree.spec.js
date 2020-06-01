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

import angular from 'angular';
import 'angular-mocks';

describe('The tree rendered by the tree directive', () => {
  const defaultItems = [
    {
      title: 'Item 1',
      items: [
        {
          title: 'Item 1.1',
        }, {
          title: 'Item 1.2',
          collapsed: true,
          items: [
            {
              title: 'Item 1.2.1',
            }, {
              title: 'Item 1.2.2',
            }, {
              title: 'Item 1.2.3',
            },
          ],
        }, {
          title: 'Item 1.3',
        },
      ],
    }, {
      title: 'Item 2',
    }, {
      title: 'Item 3',
    },
  ];

  let $rootScope;
  let $scope;
  let $compile;

  beforeEach(angular.mock.module('hippo-cm.ui.tree'));

  beforeEach(inject((_$rootScope_, _$compile_) => {
    $rootScope = _$rootScope_;
    $compile = _$compile_;
  }));

  function createTreeAndController(items) {
    $scope = $rootScope.$new();

    $scope.options = {};
    $scope.selectedItem = {};
    $scope.items = items || $j.extend(true, {}, defaultItems);

    const $element = angular.element(
      `<div hippo-tree items="items" options="options" selected-item="selectedItem">
        <a data-ng-click="hippoTree.toggle(this)">{{ item.title }}</a>
      </div>`);
    $compile($element)($scope);
    $scope.$digest();
    const tree = $element.find('div.angular-ui-tree > ol.angular-ui-tree-nodes');
    const ctrl = $element.controller('hippoTree');

    return { tree, ctrl };
  }

  it('should be created', () => {
    const { tree } = createTreeAndController();
    expect(tree.length).toEqual(1);
  });

  it('should contain 3 root nodes', () => {
    const { tree } = createTreeAndController();
    expect(tree.children('li').length).toEqual(3);
  });

  it('should contain a first item with subitems', () => {
    const { tree } = createTreeAndController();
    expect(tree.children('li').eq(0).children('ol').children('li').length).toEqual(3);
  });

  it('should contain 2 leaf nodes at root level', () => {
    const { tree } = createTreeAndController();

    let itemsWithoutChildren = 0;
    tree.children('li').each((index, el) => {
      const numChildren = $('ol', el).children('li').length;
      if (!numChildren) {
        itemsWithoutChildren += 1;
      }
    });

    expect(itemsWithoutChildren).toBe(2);
  });

  it('should have a second node with the visible label \'Item 2\'', () => {
    const { tree } = createTreeAndController();
    expect(tree.children('li').eq(1).find('a').text()).toBe('Item 2');
  });

  it('should not render children of collapsed node \'Item 1.2\'', () => {
    const { tree } = createTreeAndController();
    const item1 = tree.children('li').eq(0);
    const item12 = item1.find('ol.angular-ui-tree-nodes > li').eq(1);
    expect(item12.find('ol.angular-ui-tree-nodes > li').length).toBe(0);
  });

  it('should render children of expanded node \'Item 1.2\'', () => {
    const items = $j.extend(true, {}, defaultItems);
    items[0].items[1].collapsed = false;

    const { tree } = createTreeAndController(items);
    const item1 = tree.children('li').eq(0);
    const item12 = item1.find('ol.angular-ui-tree-nodes > li').eq(1);
    expect(item12.find('ol.angular-ui-tree-nodes > li').length).toBe(3);
  });

  it('should keep the collapsed states of the tree and the model in sync', () => {
    const { tree } = createTreeAndController();
    const item1 = tree.children('li').eq(0);
    const item12 = item1.find('ol.angular-ui-tree-nodes > li').eq(1);
    const item12Toggle = item12.find('a');
    const item12Model = $scope.items[0].items[1];

    expect(item12Model.collapsed).toBe(true);
    item12Toggle.click();
    expect($scope.items[0].items[1].collapsed).toBe(false);
    item12Toggle.click();
    expect(item12Model.collapsed).toBe(true);
  });
});
