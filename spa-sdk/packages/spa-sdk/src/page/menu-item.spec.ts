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

import { MenuItemFactory, MenuItemImpl, MenuItemModel, MenuItem } from './menu-item';
import { LinkFactory } from './link-factory';
import { TYPE_LINK_INTERNAL } from './link';

let linkFactory: jest.Mocked<LinkFactory>;
let menuItemFactory: jest.MockedFunction<MenuItemFactory>;

const model = {
  childMenuItems: [],
  depth: 1,
  expanded: true,
  name: 'something',
  parameters: {},
  repositoryBased: true,
  selected: true,
  links: { site: { href: 'url', type: TYPE_LINK_INTERNAL } },
} as MenuItemModel;

function createMenuItem(menuItemModel = model) {
  return new MenuItemImpl(menuItemModel, linkFactory, menuItemFactory);
}

beforeEach(() => {
  linkFactory = { create: jest.fn() } as unknown as typeof linkFactory;
  menuItemFactory = jest.fn();
});

describe('MenuItemImpl', () => {
  let item: MenuItem;

  beforeEach(() => {
    item = createMenuItem();
  });

  describe('getChildren', () => {
    it('should return child items', () => {
      menuItemFactory.mockReturnValueOnce(item);
      const parent = createMenuItem({ ...model, childMenuItems: [model] });

      expect(parent.getChildren()).toEqual([item]);
    });
  });

  describe('getDepth', () => {
    it('should return an item depth', () => {
      expect(item.getDepth()).toBe(1);
    });
  });

  describe('getLink', () => {
    it('should return an item link', () => {
      expect(item.getLink()).toEqual({ href: 'url', type: TYPE_LINK_INTERNAL });
    });
  });

  describe('getName', () => {
    it('should return an item name', () => {
      expect(item.getName()).toBe('something');
    });
  });

  describe('getParameters', () => {
    it('should return item parameters', () => {
      expect(item.getParameters()).toBe(model.parameters);
    });
  });

  describe('getUrl', () => {
    it('should return an item URL', () => {
      linkFactory.create.mockReturnValueOnce('url');

      expect(item.getUrl()).toBe('url');
      expect(linkFactory.create).toBeCalledWith({ href: 'url', type: TYPE_LINK_INTERNAL });
    });
  });

  describe('isExpanded', () => {
    it('should return true', () => {
      expect(item.isExpanded()).toBe(true);
    });

    it('should return false', () => {
      item = createMenuItem({ ...model, expanded: false });
      expect(item.isExpanded()).toBe(false);
    });
  });

  describe('isRepositoryBased', () => {
    it('should return true', () => {
      expect(item.isRepositoryBased()).toBe(true);
    });

    it('should return false', () => {
      item = createMenuItem({ ...model, repositoryBased: false });
      expect(item.isRepositoryBased()).toBe(false);
    });
  });

  describe('isSelected', () => {
    it('should return true', () => {
      expect(item.isSelected()).toBe(true);
    });

    it('should return false', () => {
      item = createMenuItem({ ...model, selected: false });
      expect(item.isSelected()).toBe(false);
    });
  });
});
