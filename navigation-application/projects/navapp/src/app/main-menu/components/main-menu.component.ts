/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import {
  AfterViewInit,
  Component,
  ElementRef,
  HostBinding,
  HostListener,
  OnInit,
  ViewChild,
} from '@angular/core';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { Observable, of } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

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
export class MainMenuComponent implements OnInit, AfterViewInit {
  menuItems$: Observable<MenuItem[]>;
  isHelpToolbarOpened = false;
  isUserToolbarOpened = false;
  availableHeightForScrollableArea = 0;

  @ViewChild('progressBar', { static: false })
  readonly progressBar: ElementRef<HTMLElement>;

  @ViewChild('bottomElements', { static: false })
  readonly bottomElements: ElementRef<HTMLElement>;

  constructor(
    private readonly el: ElementRef<HTMLElement>,
    private readonly menuStateService: MenuStateService,
    private readonly qaHelperService: QaHelperService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly navigationService: NavigationService,
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
    this.menuItems$ = this.menuStateService.menu$;
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.availableHeightForScrollableArea = this.calculateAvailableHeightForScrollableArea());
  }

  toggle(): void {
    this.menuStateService.toggle();
  }

  onMenuItemClick(item: MenuItem): void {
    this.isHelpToolbarOpened = false;
    this.isUserToolbarOpened = false;
    this.selectMenuItem(item);
  }

  onHelpMenuItemClick(): void {
    this.menuStateService.closeDrawer();
    this.isUserToolbarOpened = false;
    this.isHelpToolbarOpened = true;
  }

  onUserMenuItemClick(): void {
    this.menuStateService.closeDrawer();
    this.isHelpToolbarOpened = false;
    this.isUserToolbarOpened = true;
  }

  isMenuItemHighlighted(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemHighlighted(item);
  }

  getQaClass(item: MenuItem | string): string {
    return this.qaHelperService.getMenuItemClass(item);
  }

  @HostListener('window:resize')
  onResize(): void {
    this.availableHeightForScrollableArea = this.calculateAvailableHeightForScrollableArea();
  }

  private selectMenuItem(item: MenuItem): void {
    if (item instanceof MenuItemLink) {
      this.navigationService.navigateByNavItem(item.navItem, NavigationTrigger.Menu);
      return;
    }

    if (item instanceof MenuItemContainer) {
      this.menuStateService.openDrawer(item);
    }
  }

  private calculateAvailableHeightForScrollableArea(): number {
    const occupied =
      this.bottomElements.nativeElement.clientHeight +
      this.progressBar.nativeElement.clientHeight;

    return this.el.nativeElement.clientHeight - occupied;
  }
}
