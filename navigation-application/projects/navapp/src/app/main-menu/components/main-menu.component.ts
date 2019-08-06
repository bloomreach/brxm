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
import { filter, first, switchMap, takeUntil } from 'rxjs/operators';

import { ClientAppService } from '../../client-app/services/client-app.service';
import { UserSettings } from '../../models/dto/user-settings.dto';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { GlobalSettingsService } from '../../services/global-settings.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';
import { MenuStateService } from '../services/menu-state.service';

@Component({
  selector: 'brna-main-menu',
  templateUrl: 'main-menu.component.html',
  styleUrls: ['main-menu.component.scss'],
})
export class MainMenuComponent implements OnInit, OnDestroy {
  menuItems: MenuItem[] = [];
  userSettings: UserSettings;
  isHelpToolbarOpened = false;
  isUserToolbarOpened = false;

  private homeMenuItem: MenuItemLink;
  private unsubscribe = new Subject();

  constructor(
    private menuStateService: MenuStateService,
    private qaHelperService: QaHelperService,
    private clientAppService: ClientAppService,
    private settingsService: GlobalSettingsService,
    private busyIndicatorService: BusyIndicatorService,
  ) {}

  get isBusyIndicatorVisible(): boolean {
    return this.busyIndicatorService.isVisible;
  }

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
    this.userSettings = this.settingsService.userSettings;

    this.extractMenuItems();
    this.activateHomeMenuItem();
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
    this.isHelpToolbarOpened = false;
    this.isUserToolbarOpened = false;
    this.selectMenuItem(item);
  }

  onHelpMenuItemClick(event: MouseEvent): void {
    event.stopImmediatePropagation();
    this.menuStateService.closeDrawer();
    this.isUserToolbarOpened = false;
    this.isHelpToolbarOpened = true;
  }

  onUserMenuItemClick(event: MouseEvent): void {
    event.stopImmediatePropagation();
    this.menuStateService.closeDrawer();
    this.isHelpToolbarOpened = false;
    this.isUserToolbarOpened = true;
  }

  selectMenuItem(item: MenuItem): void {
    this.isUserToolbarOpened = false;
    if (item instanceof MenuItemLink) {
      this.menuStateService.activateMenuItem(item.appId, item.appPath);
      return;
    }

    if (item instanceof MenuItemContainer) {
      this.menuStateService.openDrawer(item);
    }
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

  private extractMenuItems(): void {
    this.menuStateService.menu$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(menuItems => {
        this.homeMenuItem = menuItems[0] as MenuItemLink;
        this.menuItems = menuItems.slice(1);
      });
  }

  private activateHomeMenuItem(): void {
    this.menuStateService.menu$
      .pipe(
        takeUntil(this.unsubscribe),
        switchMap(() => this.clientAppService.connectionEstablished$),
        filter(app => app.id === this.homeMenuItem.appId),
      )
      .subscribe(() => {
        this.selectMenuItem(this.homeMenuItem);
      });
  }
}
