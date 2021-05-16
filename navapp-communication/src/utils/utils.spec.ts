/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { mergeIntersecting } from './utils';

describe('mergeIntersecting', () => {
  it('creates a new object from 2 other objects', () => {
    const obj1 = {
      test1(): void {},
    };
    const obj2 = {
      test1(): void {},
    };

    expect(mergeIntersecting(obj1, obj2)).not.toBe(obj1 || obj2);
  });

  it('merges intersecting enumerable own properties on 2 objects', () => {
    const test = () => 1 + 1;
    const test2 = () => 2 + 2;
    const obj1 = {
      myMethod: () => {},
      test,
    };
    const obj2 = {
      myOtherMethod: () => {},
      test: test2,
    };

    const merged = mergeIntersecting(obj1, obj2);

    expect((merged as any).test).toBe(test2);
    expect('myMethod' in merged).toBe(true);
    expect('myOtherMethod' in merged).toBe(false);
  });
});
