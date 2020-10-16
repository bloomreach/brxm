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

import { MenuImpl, MenuModel, TYPE_MENU, isMenu } from './menu';
import { MenuItemFactory, MenuItemModel, MenuItem } from './menu-item';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection } from './meta-collection';

let menuItemFactory: jest.MockedFunction<MenuItemFactory>;
let metaFactory: jest.MockedFunction<MetaCollectionFactory>;

const itemModel = {} as MenuItemModel;
const metaModel = {};

const model = {
  data: {
    name: 'something',
    selectSiteMenuItem: null,
    siteMenuItems: [],
  },
  meta: metaModel,
  type: TYPE_MENU,
} as MenuModel;

function createMenu(menuModel = model) {
  return new MenuImpl(menuModel, metaFactory, menuItemFactory);
}

beforeEach(() => {
  menuItemFactory = jest.fn();
  metaFactory = jest.fn();
});

describe('MenuImpl', () => {
  describe('getItems', () => {
    it('should return menu items', () => {
      const item = {} as MenuItem;
      menuItemFactory.mockReturnValueOnce(item);

      const menu = createMenu({ ...model, data: { ...model.data, siteMenuItems: [itemModel] } });

      expect(menu.getItems()).toEqual([item]);
    });
  });

  describe('getMeta', () => {
    it('should return a meta-data array', () => {
      const meta = {} as MetaCollection;
      metaFactory.mockReturnValueOnce(meta);

      const menu = createMenu({ ...model, meta: metaModel });

      expect(metaFactory).toBeCalledWith(metaModel);
      expect(menu.getMeta()).toEqual(meta);
    });
  });

  describe('getName', () => {
    it('should return a menu name', () => {
      const menu = createMenu();

      expect(menu.getName()).toBe('something');
    });
  });

  describe('getSelected', () => {
    it('should return undefined when there is no selected item', () => {
      const menu = createMenu();

      expect(menu.getSelected()).toBeUndefined();
    });

    it('should return a selected item', () => {
      const item = {} as MenuItem;
      menuItemFactory.mockReturnValueOnce(item);

      const menu = createMenu({ ...model, data: { ...model.data, selectSiteMenuItem: itemModel } });

      expect(menu.getSelected()).toBe(item);
    });
  });
});

describe('isMenu', () => {
  it('should return true', () => {
    const menu = createMenu();

    expect(isMenu(menu)).toBe(true);
  });

  it('should return false', () => {
    expect(isMenu(undefined)).toBe(false);
    expect(isMenu({})).toBe(false);
  });
});
