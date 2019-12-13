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

import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Inject, OnDestroy, OnInit } from '@angular/core';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { Observable, Subject } from 'rxjs';

import { APP_BOOTSTRAPPED } from '../../bootstrap/app-bootstrapped';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { NavigationService } from '../../services/navigation.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';
import { MenuStateService } from '../services/menu-state.service';

@Component({
  selector: 'brna-main-menu',
  templateUrl: 'main-menu.component.html',
  styleUrls: ['main-menu.component.scss'],
  animations: [
    trigger('rotate-expand-collapse', [
      state('true', style({ transform: 'rotate(0)' })),
      state('false', style({ transform: 'rotate(-180deg)' })),
      transition('true <=> false', animate('300ms ease')),
    ]),
  ],
})
export class MainMenuComponent implements OnInit, OnDestroy {
  menuItems: MenuItem[] = [];
  isHelpToolbarOpened = false;
  isUserToolbarOpened = false;

  private readonly homeMenuItem: MenuItemLink;
  private readonly unsubscribe = new Subject();

  constructor(
    private readonly menuStateService: MenuStateService,
    private readonly qaHelperService: QaHelperService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly navigationService: NavigationService,
    @Inject(APP_BOOTSTRAPPED) private readonly appBootstrapped: Promise<void>,
  ) { }

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

  ngOnInit(): void {
    this.appBootstrapped.then(() => this.extractMenuItems());
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  toggle(): void {
    this.menuStateService.toggle();
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
      this.navigationService.navigateByNavItem(item.navItem, NavigationTrigger.Menu);
      return;
    }

    if (item instanceof MenuItemContainer) {
      this.menuStateService.openDrawer(item);
    }
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }

  getQaClass(item: MenuItem | string): string {
    return this.qaHelperService.getMenuItemClass(item);
  }

  private extractMenuItems(): void {
    const menu = this.menuStateService.menu;

    if (menu.length === 0) {
      return;
    }

    this.menuItems = menu;
  }
}
