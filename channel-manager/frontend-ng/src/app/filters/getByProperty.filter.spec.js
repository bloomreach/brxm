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

describe('getByProperty filter', () => {
  let getByPropertyFilter;
  const items = [
    {
      id: 1,
      title: 'Item 1',
      items: [
        {
          id: 11,
          title: 'Item 1.1',
        },
      ],
    },
    {
      id: 2,
      title: 'Item 2',
      items: [],
    },
  ];

  beforeEach(angular.mock.module('hippo-cm'));

  beforeEach(inject((_getByPropertyFilter_) => {
    getByPropertyFilter = _getByPropertyFilter_;
  }));

  it('should find an object by property', () => {
    const foundItem = {
      id: 2,
      title: 'Item 2',
      items: [],
    };
    expect(getByPropertyFilter(items, 'id', 2)).toEqual(foundItem);
  });

  it('should find an object by property in a sub item', () => {
    const foundItem = {
      id: 11,
      title: 'Item 1.1',
    };
    items[0].collapsed = true;
    expect(getByPropertyFilter(items, 'id', 11, 'items')).toEqual(foundItem);
  });
});
