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

import angular from 'angular';
import 'angular-mocks';

describe('ItemListing', () => {
  let $componentController;
  let component;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    component = $componentController('itemListing', null, { selectedItem: 'a' });
  });

  it('can have a pre-selected item', () => {
    expect(component.selectedItem).toBe('a');
  });

  it('selects an item', () => {
    component.selectItem('b');
    expect(component.selectedItem).toBe('b');
  });

  it('renders item icons', () => {
    expect(component.getItemIcon({ type: 'folder' })).toBe('folder-closed');
    expect(component.getItemIcon({ state: 'new' })).toBe('document-status-new');
    expect(component.getItemIcon({ state: 'changed' })).toBe('document-status-changed');
    expect(component.getItemIcon({ state: 'live' })).toBe('document-status-live');
    expect(component.getItemIcon({ state: 'unknown' })).toBe('document');
    expect(component.getItemIcon({})).toBe('document');
  });
});
