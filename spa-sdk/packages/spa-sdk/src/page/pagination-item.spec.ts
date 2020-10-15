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

import { PaginationItemImpl, PaginationItemModel, PaginationItem } from './pagination-item';
import { LinkFactory } from './link-factory';
import { TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL } from './link';

describe('PaginationItemImpl', () => {
  const model = {
    number: 1,
    links: {
      self: { href: 'url', type: TYPE_LINK_EXTERNAL },
      site: { href: 'url', type: TYPE_LINK_INTERNAL },
    },
  } as PaginationItemModel;

  let item: PaginationItem;
  let linkFactory: jest.Mocked<LinkFactory>;

  beforeEach(() => {
    linkFactory = { create: jest.fn() } as unknown as typeof linkFactory;
    item = new PaginationItemImpl(model, linkFactory);
  });

  describe('getNumber', () => {
    it('should return a page number', () => {
      expect(item.getNumber()).toBe(1);
    });
  });

  describe('getUrl', () => {
    it('should return an item URL', () => {
      linkFactory.create.mockReturnValueOnce('url');

      expect(item.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'url', type: TYPE_LINK_INTERNAL });
    });
  });
});
