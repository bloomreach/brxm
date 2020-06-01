/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import { MetaImpl, Meta, META_POSITION_BEGIN, TYPE_META_COMMENT, isMeta } from './meta';

describe('MetaImpl', () => {
  let meta: Meta;

  beforeEach(() => {
    meta = new MetaImpl({ data: 'some-data', type: TYPE_META_COMMENT }, META_POSITION_BEGIN);
  });

  describe('getData', () => {
    it('should return a meta data', () => {
      expect(meta.getData()).toBe('some-data');
    });
  });

  describe('getPosition', () => {
    it('should return a position', () => {
      expect(meta.getPosition()).toBe(META_POSITION_BEGIN);
    });
  });
});

describe('isMeta', () => {
  it('should return true', () => {
    const meta = new MetaImpl({ data: 'some-data', type: TYPE_META_COMMENT }, META_POSITION_BEGIN);

    expect(isMeta(meta)).toBe(true);
  });

  it('should return false', () => {
    expect(isMeta(undefined)).toBe(false);
    expect(isMeta({})).toBe(false);
  });
});
