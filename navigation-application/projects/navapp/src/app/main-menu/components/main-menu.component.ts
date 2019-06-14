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

import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { first, switchMap, takeUntil } from 'rxjs/operators';

import { ClientAppService } from '../../client-app/services';
import { UserSettings } from '../../models/dto';
import { NavAppSettingsService, QaHelperService } from '../../services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';
import { MenuStateService } from '../services';

@Component({
  selector: 'brna-main-menu',
  templateUrl: 'main-menu.component.html',
  styleUrls: ['main-menu.component.scss'],
})
export class MainMenuComponent implements OnInit, OnDestroy {
  menuItems: MenuItem[] = [];
  userSettings: UserSettings;
  isUserToolbarOpened = false;

  private homeMenuItem: MenuItemLink;
  private unsubscribe = new Subject();

  constructor(
    private menuStateService: MenuStateService,
    private qaHelperService: QaHelperService,
    private clientAppService: ClientAppService,
    private navAppSettingsService: NavAppSettingsService,
  ) {}

  get collapsed(): boolean {
    return this.menuStateService.isMenuCollapsed;
  }

  get isDrawerOpen(): boolean {
    return this.menuStateService.isDrawerOpened;
  }

  get drawerMenuItem(): MenuItemContainer {
    return this.menuStateService.drawerMenuItem;
  }

  @HostBinding('class.collapsed')
  get isCollapsed(): boolean {
    return this.collapsed;
  }

  // Should be replaced with proper routing later
  ngOnInit(): void {
    this.menuStateService.menu$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(menuItems => {
      this.homeMenuItem = menuItems[0] as MenuItemLink;
      this.menuItems = menuItems.slice(1);
    });

    this.clientAppService.connectionsEstablished$.pipe(
      first(),
      switchMap(() => this.menuStateService.menu$),
      takeUntil(this.unsubscribe),
    ).subscribe(() => this.selectMenuItem(this.homeMenuItem));

    this.userSettings = this.navAppSettingsService.userSettings;
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  toggle(): void {
    this.menuStateService.toggle();
  }

  onHomeMenuItemClick(event: MouseEvent): void {
    event.stopImmediatePropagation();
    this.selectMenuItem(this.homeMenuItem);
  }

  onMenuItemClick(event: MouseEvent, item: MenuItem): void {
    event.stopImmediatePropagation();
    this.selectMenuItem(item);
  }

  onUserMenuItemClick(event: MouseEvent): void {
    event.stopImmediatePropagation();
    this.selectUserMenuItem();
  }

  onClickedOutsideUserToolbar(): void {
    this.isUserToolbarOpened = false;
  }

  selectMenuItem(item: MenuItem): void {
    this.isUserToolbarOpened = false;
    if (item instanceof MenuItemLink) {
      this.menuStateService.setActiveItemAndNavigate(item);
      return;
    }

    if (item instanceof MenuItemContainer) {
      this.menuStateService.openDrawer(item);
    }
  }

  selectUserMenuItem(): void {
    this.isUserToolbarOpened = true;
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }

  isHomeMenuItemActive(): boolean {
    return this.isMenuItemActive(this.homeMenuItem);
  }

  getQaClass(item: MenuItem | string): string {
    return this.qaHelperService.getMenuItemClass(item);
  }
}
