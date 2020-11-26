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

import { injectable, inject } from 'inversify';
import { MenuItemFactory, MenuItemModel, MenuItem } from './menu-item';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollectionModel, MetaCollection } from './meta-collection';

export const MenuModelToken = Symbol.for('MenuModelToken');

/**
 * A manage menu button.
 */
export const TYPE_MANAGE_MENU_BUTTON = 'EDIT_MENU_LINK';

export const TYPE_MENU = 'menu';

interface MenuDataModel {
  name: string;
  selectSiteMenuItem: MenuItemModel | null;
  siteMenuItems: MenuItemModel[];
}

/**
 * Essentials component menu model.
 */
export interface MenuModel {
  data: MenuDataModel;
  meta: MetaCollectionModel;
  type: typeof TYPE_MENU;
}

export interface Menu {
  /**
   * @return The menu items.
   */
  getItems(): MenuItem[];

  /**
   * @return The menu meta-data collection.
   */
  getMeta(): MetaCollection;

  /**
   * @return The current menu item.
   */
  getSelected(): MenuItem | undefined;
}

@injectable()
export class MenuImpl implements Menu {
  private items: MenuItem[];

  private meta: MetaCollection;

  private selected?: MenuItem;

  constructor(
    @inject(MenuModelToken) protected model: MenuModel,
    @inject(MetaCollectionFactory) metaFactory: MetaCollectionFactory,
    @inject(MenuItemFactory) menuItemFactory: MenuItemFactory,
  ) {
    this.items = model.data.siteMenuItems.map(menuItemFactory);
    this.meta = metaFactory(model.meta);
    this.selected = model.data.selectSiteMenuItem
      ? menuItemFactory(model.data.selectSiteMenuItem)
      : undefined;
  }

  getItems(): MenuItem[] {
    return this.items;
  }

  getMeta(): MetaCollection {
    return this.meta;
  }

  getName(): string {
    return this.model.data.name;
  }

  getSelected(): MenuItem | undefined {
    return this.selected;
  }
}

/**
 * Checks whether a value is a menu.
 * @param value The value to check.
 */
export function isMenu(value: unknown): value is Menu {
  return value instanceof MenuImpl;
}
