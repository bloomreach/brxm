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
import { Builder } from './factory';
import { LinkFactory } from './link-factory';
import { Link } from './link';

export const MenuItemFactory = Symbol.for('MenuItemFactory');
export const MenuItemModelToken = Symbol.for('MenuItemModelToken');

export type MenuItemFactory = Builder<[MenuItemModel], MenuItem>;

/**
 * @hidden
 */
type MenuItemLinks = 'site';

/**
 * Essentials component menu item model.
 * @hidden
 */
export interface MenuItemModel {
  childMenuItems: MenuItemModel[];
  depth: number;
  expanded: boolean;
  name: string;
  parameters: object;
  repositoryBased: boolean;
  selected: boolean;
  links: Partial<Record<MenuItemLinks, Link>>;
}

export interface MenuItem {
  /**
   * @return The child items.
   */
  getChildren(): MenuItem[];

  /**
   * @return The menu item depth level.
   */
  getDepth(): number;

  /**
   * @return The menu item link.
   */
  getLink(): Link | undefined;

  /**
   * @return The menu item name.
   */
  getName(): string;

  /**
   * @return The menu item parameters.
   */
  getParameters(): object;

  /**
   * @return The menu item url.
   */
  getUrl(): string | undefined;

  /**
   * @return Whether the menu item is expanded.
   */
  isExpanded(): boolean;

  /**
   * @return Whether the menu item is repository based.
   */
  isRepositoryBased(): boolean;

  /**
   * @return Whether the menu item is selected.
   */
  isSelected(): boolean;
}

@injectable()
export class MenuItemImpl implements MenuItem {
  private children: MenuItem[];

  constructor(
    @inject(MenuItemModelToken) protected model: MenuItemModel,
    @inject(LinkFactory) private linkFactory: LinkFactory,
    @inject(MenuItemFactory) menuItemFactory: MenuItemFactory,
  ) {
    this.children = model.childMenuItems.map(menuItemFactory);
  }

  getChildren(): MenuItem[] {
    return this.children;
  }

  getDepth(): number {
    return this.model.depth;
  }

  getLink(): Link | undefined {
    return this.model.links.site;
  }

  getName(): string {
    return this.model.name;
  }

  getParameters(): object {
    return this.model.parameters;
  }

  getUrl(): string | undefined {
    return this.model.links.site && this.linkFactory.create(this.model.links.site);
  }

  isExpanded(): boolean {
    return this.model.expanded;
  }

  isRepositoryBased(): boolean {
    return this.model.repositoryBased;
  }

  isSelected(): boolean {
    return this.model.selected;
  }
}
