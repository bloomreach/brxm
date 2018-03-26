/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('siteMapListingController', () => {
  let $ctrl;

  const items = [{ a: 'one' }, { a: 'two' }];
  const filteredFields = ['a'];
  let onFilter;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.search');

    onFilter = jasmine.createSpy('onFilter');

    inject(($componentController) => {
      $ctrl = $componentController('search', {}, {
        items,
        filteredFields,
        onFilter,
        itemName: 'thing',
        itemsName: 'things',
      });
    });
  });

  it('filters items', () => {
    $ctrl.keywords = 'w';
    $ctrl.filterItems();

    expect($ctrl.hits).toEqual(1);
    expect($ctrl.onFilter).toHaveBeenCalledWith({ filteredItems: [{ a: 'two' }] });
  });

  it('clears the filter', () => {
    $ctrl.keywords = 'b';
    $ctrl.clearFilter();

    expect($ctrl.keywords).toEqual('');
    expect($ctrl.hits).toEqual(items.length);
    expect(onFilter).toHaveBeenCalledWith({ filteredItems: items });
  });

  it('clears the filter on changes', () => {
    $ctrl.keywords = 'b';
    $ctrl.$onChanges();

    expect($ctrl.keywords).toEqual('');
    expect($ctrl.hits).toEqual(items.length);
    expect(onFilter).toHaveBeenCalledWith({ filteredItems: items });
  });

  it('returns translationData containing the name of the items, single and plural', () => {
    expect($ctrl.translationData.itemName).toEqual('thing');
    expect($ctrl.translationData.itemsName).toEqual('things');
  });

  it('returns translationData containing the total number of items and the number of hits', () => {
    $ctrl.items = [];
    $ctrl.hits = 0;
    expect($ctrl.translationData.total).toEqual(0);
    expect($ctrl.translationData.hits).toEqual(0);

    $ctrl.items = ['one', 'two'];
    expect($ctrl.translationData.total).toEqual(2);
    expect($ctrl.translationData.hits).toEqual(0);

    $ctrl.hits = 1;
    expect($ctrl.translationData.total).toEqual(2);
    expect($ctrl.translationData.hits).toEqual(1);
  });
});
