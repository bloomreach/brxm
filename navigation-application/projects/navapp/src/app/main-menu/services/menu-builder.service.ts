/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Injectable } from '@angular/core';
import { NavItem } from '@bloomreach/navapp-communication';

import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';

import { MenuStructureService } from './menu-structure.service';

@Injectable()
export class MenuBuilderService {
  constructor(private readonly menuStructureService: MenuStructureService) {}

  buildMenu(navItems: NavItem[]): MenuItem[] {
    const menu = this.menuStructureService.getMenuStructure();

    this.applyNavItems(menu, navItems);

    const menuWithoutEmptyLeaves = this.removeEmptyLeaves(menu);

    return this.reduceUnnecessaryNesting(menuWithoutEmptyLeaves);
  }

  private applyNavItems(menu: MenuItem[], navItems: NavItem[]): void {
    const menuItemLinks = this.getMenuItemLinks(menu);
    navItems.forEach(navItem => {
      const correspondingMenuItem = menuItemLinks.find(i => i.id === navItem.id);

      if (correspondingMenuItem) {
        correspondingMenuItem.navItem = navItem;
        return;
      }

      if (navItem.displayName) {
        const menuItem = new MenuItemLink(navItem.id, navItem.displayName);
        this.menuStructureService.addExtension(menuItem);
        menuItem.navItem = navItem;
      }
    });
  }

  private removeEmptyLeaves(menu: MenuItem[]): MenuItem[] {
    return menu.filter(item => {
      if (item instanceof MenuItemLink) {
        return !!item.navItem;
      }

      if (item instanceof MenuItemContainer) {
        item.children = this.removeEmptyLeaves(item.children);
        return Array.isArray(item.children) && item.children.length > 0;
      }

      throw new Error('MenuItem has unknown type.');
    });
  }

  private reduceUnnecessaryNesting(items: MenuItem[]): MenuItem[] {
    return items.map(item => {
      if (!(item instanceof MenuItemContainer)) {
        return item;
      }

      item.children = this.reduceUnnecessaryNesting(item.children);

      if (item.children.length === 1) {
        return this.levelUpLink(item.children[0] as MenuItemLink, item);
      }

      return item;
    });
  }

  private levelUpLink(link: MenuItemLink, linkContainer: MenuItemContainer): MenuItemLink {
    link.icon = linkContainer.icon;

    return link;
  }

  private getMenuItemLinks(menu: MenuItem[]): MenuItemLink[] {
    return menu.reduce((leafs, item) => {
      if (item instanceof MenuItemContainer) {
        return leafs.concat(this.getMenuItemLinks(item.children));
      }
      leafs.push(item);
      return leafs;
    }, []);
  }
}
