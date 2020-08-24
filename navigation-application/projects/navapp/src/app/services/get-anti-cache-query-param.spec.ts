/*!
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

import { getAntiCacheQueryParam } from './get-anti-cache-query-param';

describe('getAntiCacheQueryParam', () => {
  it('should return anti cache query param from the location url', () => {
    const expected = 'antiCache=some-hash';

    const actual = getAntiCacheQueryParam('/some/path?antiCache=some-hash');

    expect(actual).toBe(expected);
  });

  it('should create anti cache query param if the location url does not contain antiCache', () => {
    const expected = jasmine.stringMatching(/^antiCache=\d+/);

    const actual = getAntiCacheQueryParam('/some/path');

    expect(actual).toEqual(expected);
  });

  it('should be able to find the antiCache query param if there are a few query params in the url', () => {
    const expected = 'antiCache=some-hash';

    const actual = getAntiCacheQueryParam('/some/path?param1=value1&antiCache=some-hash&param2=value2');

    expect(actual).toBe(expected);
  });
});
