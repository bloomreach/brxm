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

describe('startWithSlashFilter', () => {
  'use strict';

  let startWithSlashFilter;

  beforeEach(() => {
    module('hippo-cm');

    inject((_startWithSlashFilter_) => {
      startWithSlashFilter = _startWithSlashFilter_;
    });
  });

  it('adds an initial slash when the input does not start with a slash', () => {
    expect(startWithSlashFilter('test')).toBe('/test');
  });

  it('doesn\'t add an initial slash when the input already starts with a slash', () => {
    expect(startWithSlashFilter('/test')).toBe('/test');
  });

  it('doesn\'t add an initial slash when the input is a slash', () => {
    expect(startWithSlashFilter('/')).toBe('/');
  });

  it('returns a slash when the input is empty', () => {
    expect(startWithSlashFilter('')).toBe('/');
  });
});
