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

describe('PathService', () => {
  'use strict';

  let PathService;

  beforeEach(() => {
    inject((_PathService_) => {
      PathService = _PathService_;
    });
  });

  it('concatenates two paths', () => {
    expect(PathService.concatPaths('one', 'two')).toEqual('one/two');
    expect(PathService.concatPaths('/one', 'two')).toEqual('/one/two');
    expect(PathService.concatPaths('/one/', 'two')).toEqual('/one/two');
    expect(PathService.concatPaths('one', '/two')).toEqual('one/two');
    expect(PathService.concatPaths('one', '/two/')).toEqual('one/two/');
    expect(PathService.concatPaths('/one/', '/two/')).toEqual('/one/two/');
    expect(PathService.concatPaths('/', 'two')).toEqual('/two');
    expect(PathService.concatPaths('/', '/two')).toEqual('/two');
    expect(PathService.concatPaths('one', '/')).toEqual('one/');
    expect(PathService.concatPaths('one/', '/')).toEqual('one/');
    expect(PathService.concatPaths('/', '/')).toEqual('/');
    expect(PathService.concatPaths(' one ', ' two ')).toEqual('one/two');
  });

  it('ignores falsy paths', () => {
    expect(PathService.concatPaths()).toEqual('');
    expect(PathService.concatPaths('', '')).toEqual('');
    expect(PathService.concatPaths('')).toEqual('');
    expect(PathService.concatPaths('one')).toEqual('one');
    expect(PathService.concatPaths('', 'two ')).toEqual('two');
    expect(PathService.concatPaths('one', '')).toEqual('one');
    expect(PathService.concatPaths(undefined, 'two')).toEqual('two');
    expect(PathService.concatPaths('one', '')).toEqual('one');
    expect(PathService.concatPaths('', 'two')).toEqual('two');
  });
});

