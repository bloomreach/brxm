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

import { Component, HostBinding, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { first, switchMap } from 'rxjs/operators';

import { ClientAppService } from '../../client-app/services';
import { QaHelperService } from '../../services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';
import { MenuStateService } from '../services';

@Component({
  selector: 'brna-main-menu',
  templateUrl: 'main-menu.component.html',
  styleUrls: ['main-menu.component.scss'],
})
export class MainMenuComponent implements OnInit {
  get collapsed(): boolean {
    return this.menuStateService.isMenuCollapsed;
  }

  get menu$(): Observable<MenuItem[]> {
    return this.menuStateService.menu$;
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
  constructor(
    private menuStateService: MenuStateService,
    private qaHelperService: QaHelperService,
    private clientAppService: ClientAppService,
  ) {}

  // Should be replaced with proper routing later
  ngOnInit(): void {
    this.clientAppService.connectionsEstablished$
      .pipe(
        first(),
        switchMap(() => this.menu$),
      )
      .subscribe(items => this.selectMenuItem(items[0]));
  }

  toggle(): void {
    this.menuStateService.toggle();
  }

  isRippleDisabled(item: MenuItemContainer): boolean {
    return item instanceof MenuItemContainer;
  }

  onMenuItemClick(event: MouseEvent, item: MenuItem): void {
    event.stopImmediatePropagation();
    this.selectMenuItem(item);
  }

  selectMenuItem(item: MenuItem): void {
    if (item instanceof MenuItemLink) {
      this.menuStateService.setActiveItem(item);
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
}
