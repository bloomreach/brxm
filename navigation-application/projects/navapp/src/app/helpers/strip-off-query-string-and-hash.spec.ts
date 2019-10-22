/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { stripOffQueryStringAndHash } from './strip-off-query-string-and-hash';

describe('stripOffQueryStringAndHash', () => {
  it('should strip off a query string', () => {
    const expected = 'some/path';

    const actual = stripOffQueryStringAndHash('some/path?query=string');

    expect(actual).toBe(expected);
  });

  it('should strip off a query string with a hash', () => {
    const expected = 'some/path';

    const actual = stripOffQueryStringAndHash('some/path?query=string#someHash');

    expect(actual).toBe(expected);
  });

  it('should strip off a hash', () => {
    const expected = 'some/path';

    const actual = stripOffQueryStringAndHash('some/path#someHash');

    expect(actual).toBe(expected);
  });

  it('should strip off a hash', () => {
    const expected = 'some/path';

    const actual = stripOffQueryStringAndHash('some/path#someHash');

    expect(actual).toBe(expected);
  });
});
