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

import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter, map, shareReplay, takeUntil } from 'rxjs/operators';

import { NavConfigService } from '../../services/nav-config.service';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuBuilderService } from './menu-builder.service';

@Injectable()
export class MenuStateService implements OnDestroy {
  private readonly menusStream$: Observable<MenuItem[]>;
  private menu: MenuItem[];
  private breadcrumbs = new BehaviorSubject<MenuItem[]>([]);
  private collapsed = true;
  private currentDrawerMenuItem: MenuItemContainer;
  private unsubscribe = new Subject();

  constructor(
    private menuBuilderService: MenuBuilderService,
    private navConfigService: NavConfigService,
  ) {
    this.menusStream$ = navConfigService.navItems$.pipe(
      map(navItems => this.menuBuilderService.buildMenu(navItems)),
      takeUntil(this.unsubscribe),
      shareReplay(),
    );

    this.menusStream$.subscribe(menu => {
      if (this.menu && this.menu.length) {
        throw new Error(
          'Menu has changed. Rebuild breadcrumbs functionality must be implemented to prevent menu incorrect behavior issues.',
        );
      }

      this.menu = menu;
    });
  }

  get menu$(): Observable<MenuItem[]> {
    return this.menusStream$;
  }

  get activeMenuItem$(): Observable<MenuItemLink> {
    return this.breadcrumbs.pipe(
      map(breadcrumbs => {
        if (breadcrumbs.length === 0) {
          return undefined;
        }

        return breadcrumbs[breadcrumbs.length - 1] as MenuItemLink;
      }),
      filter(breadcrumbs => !!breadcrumbs),
    );
  }

  get isMenuCollapsed(): boolean {
    return this.collapsed;
  }

  get isDrawerOpened(): boolean {
    return !!this.currentDrawerMenuItem;
  }

  get drawerMenuItem(): MenuItemContainer {
    return this.currentDrawerMenuItem;
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  activateMenuItem(appId: string, path: string): void {
    const navItem = this.navConfigService.findNavItem(appId, path);

    if (!navItem) {
      throw new Error(`There is no nav item with appId=${appId} and path=${path}`);
    }

    this.setActiveItem(navItem.id);
  }

  isMenuItemActive(item: MenuItem): boolean {
    const currentBreadcrumbs = this.breadcrumbs.value;

    return currentBreadcrumbs.some(x => x === item);
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }

  openDrawer(item: MenuItemContainer): void {
    this.currentDrawerMenuItem = item;
  }

  closeDrawer(): void {
    this.currentDrawerMenuItem = undefined;
  }

  private setActiveItem(activeItemId: string): void {
    this.closeDrawer();
    this.breadcrumbs.next(this.buildBreadcrumbs(this.menu, activeItemId));
  }

  private buildBreadcrumbs(menu: MenuItem[], activeMenuItemId: string): MenuItem[] {
    return menu.reduce((breadcrumbs, item) => {
      if (item instanceof MenuItemContainer) {
        const subBreadcrumbs = this.buildBreadcrumbs(item.children, activeMenuItemId);

        if (subBreadcrumbs.length > 0) {
          subBreadcrumbs.unshift(item);
          breadcrumbs = breadcrumbs.concat(subBreadcrumbs);
        }
      } else if (item.id === activeMenuItemId) {
        breadcrumbs.push(item);
      }

      return breadcrumbs;
    }, []);
  }
}
