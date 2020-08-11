/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { PageMeta } from './page-meta';

describe('PageMeta', () => {
  let meta;

  beforeEach(() => {
    meta = new PageMeta({
      'HST-Path-Info': 'path-info',
      'HST-Channel-Id': 'channel-id',
      'HST-Context-Path': 'context-path',
      'HST-Experience-Page': 'true',
      'HST-Page-Id': 'page-id',
      'HST-Render-Variant': 'render-variant',
      'HST-SitemapItem-Id': 'sitemap-item-id',
    });
  });

  describe('addMeta', () => {
    it('should overwrite existing meta', () => {
      meta.addMeta(new PageMeta({ 'HST-Path-Info': 'something' }));

      expect(meta.getPathInfo()).toBe('something');
    });

    it('should implement fluent interface', () => {
      expect(meta.addMeta(new PageMeta({}))).toBe(meta);
    });
  });

  describe('getPathInfo', () => {
    it('should return path info', () => {
      expect(meta.getPathInfo()).toBe('path-info');
    });
  });

  describe('getChannelId', () => {
    it('should return channel id', () => {
      expect(meta.getChannelId()).toBe('channel-id');
    });
  });

  describe('getContextPath', () => {
    it('should return context path', () => {
      expect(meta.getContextPath()).toBe('context-path');
    });
  });

  describe('getPageId', () => {
    it('should return page id', () => {
      expect(meta.getPageId()).toBe('page-id');
    });
  });

  describe('getRenderVariant', () => {
    it('should return render variant', () => {
      expect(meta.getRenderVariant()).toBe('render-variant');
    });
  });

  describe('getSiteMapItemId', () => {
    it('should return the sitemap item id', () => {
      expect(meta.getSiteMapItemId()).toBe('sitemap-item-id');
    });
  });

  describe('isXPage', () => {
    it('should return true if page is an experience page', () => {
      expect(meta.isXPage()).toBe(true);
    });
  });
});
