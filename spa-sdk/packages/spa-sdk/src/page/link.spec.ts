/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { isLink, TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL, TYPE_LINK_RESOURCE, TYPE_LINK_UNKNOWN } from './link';

describe('isLink', () => {
  it('should be a link', () => {
    expect(isLink({ href: 'something' })).toBe(true);
    expect(isLink({ href: '' })).toBe(true);
  });

  it('should be an internal link', () => {
    expect(isLink({ type: TYPE_LINK_INTERNAL })).toBe(true);
  });

  it('should be an external link', () => {
    expect(isLink({ type: TYPE_LINK_EXTERNAL })).toBe(true);
  });

  it('should be a resource link', () => {
    expect(isLink({ type: TYPE_LINK_RESOURCE })).toBe(true);
  });

  it('should be an unknown link', () => {
    expect(isLink({ type: TYPE_LINK_UNKNOWN })).toBe(true);
  });

  it('should not be a link', () => {
    expect(isLink(undefined)).toBe(false);
    expect(isLink({})).toBe(false);
  });
});
