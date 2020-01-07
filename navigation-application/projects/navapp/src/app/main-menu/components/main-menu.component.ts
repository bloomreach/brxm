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
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { BehaviorSubject, fromEvent, Observable, of, Subject } from 'rxjs';
import { map, startWith, takeUntil } from 'rxjs/operators';

import { normalizeWheelEvent } from '../../helpers/normalize-wheel-event';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { NavigationService } from '../../services/navigation.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { WindowRef } from '../../shared/services/window-ref.service';
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
export class MainMenuComponent implements OnInit, AfterViewInit, OnDestroy {
  menuItems: MenuItem[] = [];
  isHelpToolbarOpened = false;
  isUserToolbarOpened = false;

  readonly height = {
    available: 0,
    bottom: 0,
    menu: 0,
    occupied: 0,
  };
  readonly menuOffsetTop$ = new BehaviorSubject(0);
  readonly transitionClass$ = new BehaviorSubject('onload-transition');

  private readonly unsubscribe = new Subject();

  @ViewChild('arrowDown', { static: false })
  private readonly arrowDown: ElementRef<HTMLElement>;
  @ViewChild('arrowUp', { static: false })
  private readonly arrowUp: ElementRef<HTMLElement>;
  @ViewChild('bottomElements', { static: false })
  private readonly bottomElements: ElementRef<HTMLElement>;
  @ViewChild('menu', { static: false })
  private readonly menu: ElementRef<HTMLElement>;
  @ViewChild('progressBar', { static: false })
  private readonly progressBar: ElementRef<HTMLElement>;

  constructor(
    private readonly menuStateService: MenuStateService,
    private readonly qaHelperService: QaHelperService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly navigationService: NavigationService,
    private readonly windowRef: WindowRef,
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
    this.menuItems = this.menuStateService.menu;
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMenu());
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  toggle(): void {
    this.menuStateService.toggle();
  }

  initMenu(): void {
    this.height.bottom = this.bottomElements.nativeElement.scrollHeight;
    this.height.occupied = this.progressBar.nativeElement.scrollHeight + this.height.bottom;
    this.height.available = this.windowRef.nativeWindow.innerHeight - this.height.occupied;

    fromEvent(this.windowRef.nativeWindow, 'resize')
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(this.onResize.bind(this));

    fromEvent<MouseEvent>(this.arrowDown.nativeElement, 'click')
      .pipe(takeUntil(this.unsubscribe))
      .subscribe((event: MouseEvent) => {
        event.preventDefault();
        this.transitionClass$.next('click-transition');
        this.setMenuOffsetTop(this.menuOffsetTop$.value + this.height.available);
      });

    fromEvent<MouseEvent>(this.arrowUp.nativeElement, 'click')
      .pipe(takeUntil(this.unsubscribe))
      .subscribe((event: MouseEvent) => {
        event.preventDefault();
        this.transitionClass$.next('click-transition');
        this.setMenuOffsetTop(this.menuOffsetTop$.value - this.height.available);
      });

    fromEvent<WheelEvent>(this.menu.nativeElement, 'wheel')
      .pipe(takeUntil(this.unsubscribe))
      .subscribe((event: WheelEvent) => {
        event.preventDefault();
        if (this.getMenuHeight() > this.height.available) {
          this.transitionClass$.next('wheel-transition');
          const normalized = normalizeWheelEvent(event);
          this.setMenuOffsetTop(this.menuOffsetTop$.value + normalized.y);
        }
      });
  }

  moveUpEnabled(): boolean {
    return this.menuOffsetTop$.value > 0;
  }

  moveDownEnabled(): boolean {
    return this.height.available > 0 &&
           this.height.available < this.getMenuHeight() - this.menuOffsetTop$.value;
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

  isMenuItemDisabled(item: MenuItem): Observable<boolean> {
    if (item instanceof MenuItemLink) {
      return item.navItem.active$.pipe(
        startWith(false),
        map(x => !x),
      );
    }

    return of(false);
  }

  getQaClass(item: MenuItem | string): string {
    return this.qaHelperService.getMenuItemClass(item);
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

  private onResize(event: any): void {
    const nextAvailableHeight = event.target.innerHeight - this.height.occupied;
    const delta = nextAvailableHeight - this.height.available;
    this.height.available = nextAvailableHeight;

    // move menu down if window grows vertically and has moved over the top
    if (this.menuOffsetTop$.value > 0 && delta > 0) {
      this.transitionClass$.next('resize-transition');
      const nextMenuOffsetTop = this.menuOffsetTop$.value - delta;
      this.setMenuOffsetTop(nextMenuOffsetTop);
    }
  }

  private setMenuOffsetTop(nextOffsetTop: number): void {
    const maxOffsetTop = this.getMenuHeight() - this.height.available;
    this.menuOffsetTop$.next(Math.min(Math.max(0, nextOffsetTop), Math.max(0, maxOffsetTop)));
  }

  /**
   * The actual menu height is not known when OnInit is called. We could simply always query
   * the native element's 'offsetHeight' property, but that would trigger unnecessary reflows,
   * so instead it's lazily instantiated and cached.
   *
   * Note: this will not work if the menu becomes dynamic, i.e. during runtime menu-items can be added/removed.
   */
  private getMenuHeight(): number {
    if (this.height.menu === 0) {
      this.height.menu = this.menu.nativeElement.offsetHeight;
    }
    return this.height.menu;
  }
}
