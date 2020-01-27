/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { isMatched, mergeSearchParams, parseUrl } from './utils';

describe('isMatched', () => {
  it.each`
    link                        | base
    ${'http://example.com'}     | ${undefined}
    ${'http://example.com'}     | ${''}
    ${'http://example.com/'}    | ${'http://example.com/'}
    ${'/'}                      | ${'http://example.com/'}
    ${'/spa/something'}         | ${'/spa'}
    ${'/spa/something?a=b'}     | ${'/spa?a=b'}
    ${'/spa/something?a'}       | ${'/spa?a'}
    ${'/spa/something?a=c'}     | ${'/spa?a'}
  `('should match "$link" with "$base"', ({ link, base }) => {
    expect(isMatched(link, base)).toBe(true);
  });

  it.each`
    link                        | base
    ${'http://example.com'}     | ${'http://example.com/'}
    ${'http://example.com'}     | ${'http://localhost:8080/'}
    ${'/spa'}                   | ${'/spa/something'}
    ${'/spa/something'}         | ${'/spa?a=b'}
    ${'/spa/something?c'}       | ${'/spa?a=b'}
    ${'/spa/something'}         | ${'/spa?a'}
  `('should not match "$link" with "$base"', ({ link, base }) => {
    expect(isMatched(link, base)).toBe(false);
  });
});

describe('mergeSearchParams', () => {
  it.each`
    source            | result
    ${['a=b', 'b=c']} | ${'a=b&b=c'}
    ${['a=b', 'a=c']} | ${'a=c'}
  `('should merge "$source" to "$result"', ({ source, result }) => {
    const [params, ...rest] = source.map((search: string) => new URLSearchParams(search));
    const merged = mergeSearchParams(params, ...rest);
    const expected = new URLSearchParams(result);

    expect(Array.from(merged.entries())).toEqual(Array.from(expected.entries()));
  });
});

describe('parseUrl', () => {
  it.each`
    url                           | hash       | origin                  | path        | pathname   | search | searchParams
    ${''}                         | ${''}      | ${''}                   | ${''}       | ${''}      | ${''}  | ${new URLSearchParams()}
    ${'/path'}                    | ${''}      | ${''}                   | ${'/path'}  | ${'/path'} | ${''}  | ${new URLSearchParams()}
    ${'http://example.com'}       | ${''}      | ${'http://example.com'} | ${''}       | ${''}      | ${''}  | ${new URLSearchParams()}
    ${'http://example.com/path'}  | ${''}      | ${'http://example.com'} | ${'/path'}  | ${'/path'} | ${''}  | ${new URLSearchParams()}
    ${'http://example.com/#hash'} | ${'#hash'} | ${'http://example.com'} | ${'/#hash'} | ${'/'}     | ${''}  | ${new URLSearchParams()}
    ${'http://example.com/path?a=b#hash'} | ${'#hash'} | ${'http://example.com'} | ${'/path?a=b#hash'} | ${'/path'} | ${'?a=b'}  | ${new URLSearchParams('a=b')}
  `('should parse "$url"', ({ url, searchParams, ...parts }) => {
    const { searchParams: parsedSearchParams, ...parsedParts }  = parseUrl(url);

    expect(parsedParts).toEqual(parts);
    expect(Array.from(parsedSearchParams.entries())).toEqual(Array.from(searchParams.entries()));
  });
});
