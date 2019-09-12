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

import { Content } from './content';
import { Meta, META_POSITION_BEGIN } from './meta';

describe('Content', () => {
  describe('getId', () => {
    it('should return a content item id', () => {
      const content = new Content({ id: 'some-id', name: 'some-name' });

      expect(content.getId()).toBe('some-id');
    });
  });

  describe('getLocale', () => {
    it('should return a content item locale', () => {
      const content = new Content({ id: 'some-id', name: 'some-name', localeString: 'some-locale' });

      expect(content.getLocale()).toBe('some-locale');
    });

    it('should return undefined when there is no locale', () => {
      const content = new Content({ id: 'some-id', name: 'some-name' });

      expect(content.getLocale()).toBeUndefined();
    });
  });

  describe('getMeta', () => {
    it('should return a meta-data array', () => {
      const meta = new Meta({ data: '', type: 'comment' }, META_POSITION_BEGIN);
      const content = new Content({ id: 'some-id', name: 'some-name' }, [meta]);

      expect(content.getMeta()).toEqual([meta]);
    });
  });

  describe('getName', () => {
    it('should return a content item name', () => {
      const content = new Content({ id: 'some-id', name: 'some-name' });

      expect(content.getName()).toBe('some-name');
    });
  });

  describe('getData', () => {
    it('should return a content item data', () => {
      const content = new Content({ id: 'some-id', name: 'some-name' });

      expect(content.getData()).toEqual({ id: 'some-id', name: 'some-name' });
    });
  });
});
