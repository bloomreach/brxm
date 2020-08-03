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

import { ContentImpl, ContentModel } from './content';
import { LinkFactory } from './link-factory';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection } from './meta-collection';

let linkFactory: jest.Mocked<LinkFactory>;
let metaFactory: jest.MockedFunction<MetaCollectionFactory>;

const model = {
  _links: { site: { href: 'url' } },
  id: 'some-id',
  name: 'some-name',
} as ContentModel;

function createContent(contentModel = model) {
  return new ContentImpl(contentModel, linkFactory, metaFactory);
}

beforeEach(() => {
  linkFactory = { create: jest.fn() } as unknown as typeof linkFactory;
  metaFactory = jest.fn();
});

describe('ContentImpl', () => {
  describe('getId', () => {
    it('should return a content item id', () => {
      const content = createContent();

      expect(content.getId()).toBe('some-id');
    });
  });

  describe('getLocale', () => {
    it('should return a content item locale', () => {
      const content = createContent({ ...model, localeString: 'some-locale' });

      expect(content.getLocale()).toBe('some-locale');
    });

    it('should return undefined when there is no locale', () => {
      const content = createContent();

      expect(content.getLocale()).toBeUndefined();
    });
  });

  describe('getMeta', () => {
    it('should return a meta-data array', () => {
      const metaModel = {};
      const meta = {} as MetaCollection;
      metaFactory.mockReturnValueOnce(meta);

      const content = createContent({ ...model, _meta: metaModel });

      expect(metaFactory).toBeCalledWith(metaModel);
      expect(content.getMeta()).toEqual(meta);
    });

    it('should pass an empty object if there is no meta-data', () => {
      createContent();

      expect(metaFactory).toBeCalledWith({});
    });
  });

  describe('getName', () => {
    it('should return a content item name', () => {
      const content = createContent();

      expect(content.getName()).toBe('some-name');
    });
  });

  describe('getData', () => {
    it('should return a content item data', () => {
      const content = createContent();

      expect(content.getData()).toEqual(expect.objectContaining({ id: 'some-id', name: 'some-name' }));
    });
  });

  describe('getUrl', () => {
    it('should return a content url', () => {
      const content = createContent();
      linkFactory.create.mockReturnValueOnce('url');

      expect(content.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'url' });
    });
  });
});
