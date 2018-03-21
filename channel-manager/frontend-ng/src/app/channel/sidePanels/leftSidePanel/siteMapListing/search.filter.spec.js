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

import angular from 'angular';
import 'angular-mocks';

describe('search filter', () => {
  let searchFilter;

  const andrii = {
    firstName: 'Andrii',
    lastName: 'Solod',
    qa: true,
  };
  const arthur = {
    firstName: 'Arthur',
    lastName: 'Bogaart',
  };
  const bert = {
    firstName: 'Bert',
    lastName: 'Leunis',
  };
  const mathijs = {
    firstName: 'Mathijs',
    lastName: 'den Burger',
  };
  const michael = {
    firstName: 'Michael',
    lastName: 'Metternich',
  };
  const people = [andrii, arthur, bert, mathijs, michael];

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.siteMapListing');

    inject((_searchFilter_) => {
      searchFilter = _searchFilter_;
    });
  });

  it('should return all matches for a single term', () => {
    expect(searchFilter(people, 'a', ['firstName'])).toEqual([andrii, arthur, mathijs, michael]);
    expect(searchFilter(people, 'b', ['firstName'])).toEqual([bert]);
    expect(searchFilter(people, 'AnDr', ['firstName'])).toEqual([andrii]);
    expect(searchFilter(people, 'q', ['firstName'])).toEqual([]);
  });

  it('should return all matches for a single term in all fields', () => {
    expect(searchFilter(people, 'er', ['firstName', 'lastName'])).toEqual([bert, mathijs, michael]);
  });

  it('should return all matches for multiple terms', () => {
    expect(searchFilter(people, 'a r', ['firstName'])).toEqual([andrii, arthur]);
    expect(searchFilter(people, 'er en', ['lastName'])).toEqual([mathijs]);
  });

  it('should return all matches for multiple terms in all fields', () => {
    expect(searchFilter(people, 'er s', ['firstName', 'lastName'])).toEqual([bert, mathijs]);
    expect(searchFilter(people, 'den burger', ['firstName', 'lastName'])).toEqual([mathijs]);
    expect(searchFilter(people, 'arthur metternich', ['firstName', 'lastName'])).toEqual([]);
  });

  it('should return all matches when there are no terms', () => {
    expect(searchFilter(people, '', ['firstName'])).toEqual(people);
    expect(searchFilter(people, ' ', ['firstName'])).toEqual(people);
    expect(searchFilter(people, '  ', ['firstName'])).toEqual(people);
  });

  it('should ignore non-existing fields', () => {
    expect(searchFilter(people, 'a', ['firstName', 'noSuchField'])).toEqual([andrii, arthur, mathijs, michael]);
  });

  it('should ignore non-string fields', () => {
    expect(searchFilter(people, 'a', ['qa'])).toEqual([]);
  });
});
