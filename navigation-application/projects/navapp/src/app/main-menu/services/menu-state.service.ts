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
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { CommunicationsService } from '../../services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuBuilderService } from './menu-builder.service';

@Injectable()
export class MenuStateService implements OnDestroy {
  private readonly menusStream$: Observable<MenuItem[]>;
  private currentMenu: MenuItem[];
  private breadcrumbs: MenuItem[] = [];
  private collapsed = true;
  private currentDrawerMenuItem: MenuItemContainer;
  private unsubscribe = new Subject();

  constructor(
    private menuBuilderService: MenuBuilderService,
    private communicationsService: CommunicationsService,
  ) {
    this.menusStream$ = this.menuBuilderService.buildMenu();
    this.menusStream$.pipe(takeUntil(this.unsubscribe)).subscribe(menu => {
      if (this.currentMenu && this.currentMenu.length) {
        throw new Error(
          'Menu has changed. Rebuild breadcrumbs functionality must be implemented to prevent menu incorrect behavior issues.',
        );
      }

      this.currentMenu = menu;
    });
  }

  get menu$(): Observable<MenuItem[]> {
    return this.menusStream$;
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

  setActiveItem(item: MenuItemLink): void {
    this.closeDrawer();
    this.breadcrumbs = this.buildBreadcrumbs(this.currentMenu, item);
    this.communicationsService.navigate(item.appId, item.appPath);
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.breadcrumbs.some(breadcrumbItem => breadcrumbItem === item);
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

  private buildBreadcrumbs(menu: MenuItem[], activeItem: MenuItemLink): MenuItem[] {
    return menu.reduce((breadcrumbs, item) => {
      if (item instanceof MenuItemContainer) {
        const subBreadcrumbs = this.buildBreadcrumbs(item.children, activeItem);

        if (subBreadcrumbs.length > 0) {
          subBreadcrumbs.unshift(item);
          breadcrumbs = breadcrumbs.concat(subBreadcrumbs);
        }
      }

      if (item === activeItem) {
        breadcrumbs.push(item);
      }

      return breadcrumbs;
    }, []);
  }
}
