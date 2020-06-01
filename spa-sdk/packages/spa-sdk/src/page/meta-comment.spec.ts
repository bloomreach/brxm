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

import { MetaCommentImpl, isMetaComment } from './meta-comment';
import { MetaImpl, META_POSITION_BEGIN, TYPE_META_COMMENT } from './meta';

describe('MetaCommentImpl', () => {
  describe('getData', () => {
    it('should return a data from the HTML-comment', () => {
      const meta = new MetaCommentImpl({ data: '<!-- something -->', type: 'comment' }, META_POSITION_BEGIN);
      expect(meta.getData()).toBe(' something ');
    });

    it('should fallback to the model data on invalid HTML-comment', () => {
      const meta = new MetaCommentImpl({ data: '<!-- something', type: 'comment' }, META_POSITION_BEGIN);
      expect(meta.getData()).toBe('<!-- something');
    });
  });
});

describe('isMetaComment', () => {
  it('should return true', () => {
    const meta = new MetaCommentImpl({ data: '<!-- something -->', type: 'comment' }, META_POSITION_BEGIN);

    expect(isMetaComment(meta)).toBe(true);
  });

  it('should return false', () => {
    const meta = new MetaImpl({ data: 'some-data', type: TYPE_META_COMMENT }, META_POSITION_BEGIN);

    expect(isMetaComment(undefined)).toBe(false);
    expect(isMetaComment(meta)).toBe(false);
  });
});
