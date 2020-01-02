/*!
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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { TranslateModule } from '@ngx-translate/core';
import { of, Subject } from 'rxjs';

import { APP_BOOTSTRAPPED } from '../../bootstrap/app-bootstrapped';
import { BootstrapService } from '../../bootstrap/bootstrap.service';
import { ClientAppService } from '../../client-app/services/client-app.service';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { NavigationService } from '../../services/navigation.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { WindowRef } from '../../shared/services/window-ref.service';
import { MenuItemLinkMock } from '../models/menu-item-link.mock';
import { MenuStateService } from '../services/menu-state.service';

import { MainMenuComponent } from './main-menu.component';

describe('MainMenuComponent', () => {
  let component: MainMenuComponent;
  let fixture: ComponentFixture<MainMenuComponent>;

  let menuStateService: MenuStateService;
  const menuMock = [
    new MenuItemLinkMock({ id: 'item1' }),
    new MenuItemLinkMock({ id: 'item2' }),
  ];

  menuMock[0].navItem = {
    id: 'someId',
    appIframeUrl: 'homeAppUrl',
    appPath: 'homeAppPath',
  };

  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', {
    isMenuItemActive: undefined,
    activateMenuItem: undefined,
  });
  menuStateServiceMock.menu = menuMock;

  let qaHelperService: QaHelperService;
  const qaHelperServiceMock = {
    getMenuItemClass: jasmine.createSpy('getMenuItemClass'),
  };

  let clientAppService: ClientAppService;
  const clientAppServiceMock = {
    connectionEstablished$: of(true),
  };

  const bootstrappedSuccessful$ = new Subject();
  const bootstrapServiceMock = {
    bootstrappedSuccessful$,
  };

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const navigationServiceMock = jasmine.createSpyObj('NavigationService', [
    'navigateByNavItem',
  ]);

  let appBootstrappedResolve: () => void;
  let appBootstrappedMock: Promise<void>;

  let windowRefMock: any;
  const initialWindowHeight = 800;

  beforeEach(() => {
    appBootstrappedMock = new Promise<void>((resolve, reject) => {
      appBootstrappedResolve = resolve;
    });

    windowRefMock = {
      nativeWindow: {
        innerHeight: initialWindowHeight,
        addEventListener: jasmine.createSpy('addEventListener'),
        removeEventListener: jasmine.createSpy('removeEventListener'),
      },
    };

    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      declarations: [MainMenuComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: QaHelperService, useValue: qaHelperServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: BootstrapService, useValue: bootstrapServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: APP_BOOTSTRAPPED, useValue: appBootstrappedMock },
        { provide: WindowRef, useValue: windowRefMock },
      ],
    });

    fixture = TestBed.createComponent(MainMenuComponent);

    menuStateService = fixture.debugElement.injector.get(MenuStateService);
    qaHelperService = fixture.debugElement.injector.get(QaHelperService);
    clientAppService = fixture.debugElement.injector.get(ClientAppService);

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return empty menu until app is bootstrapped', () => {
    expect(component.menuItems).toEqual([]);
  });

  it('should return all menu items when the app is bootstrapped', fakeAsync(() => {
    appBootstrappedResolve();

    tick();

    expect(component.menuItems).toEqual(menuMock);
  }));

  describe('when the app is bootstrapped', () => {
    beforeEach(async(() => {
      appBootstrappedResolve();
    }));

    it('should navigate when menu item link is clicked', () => {
      const menuItemLink = new MenuItemLinkMock();

      component.selectMenuItem(menuItemLink);

      expect(navigationServiceMock.navigateByNavItem).toHaveBeenCalledWith(menuItemLink.navItem, NavigationTrigger.Menu);
    });

    it('should not activate the home menu element until menu is emitted', () => {
      spyOn(component, 'selectMenuItem');

      expect(component.selectMenuItem).not.toHaveBeenCalled();
    });
  });

  describe('the scrolling menu', () => {
    function triggerResizeEvent(height: number): void {
      const [, resizeCallback] = windowRefMock.nativeWindow.addEventListener.calls.argsFor(0);
      windowRefMock.nativeWindow.innerHeight = height;
      resizeCallback({ target: windowRefMock.nativeWindow });
    }

    function triggerWheelEvent(deltaY: number): void {
      const menu = fixture.debugElement.query(By.css('.menu'));
      const payload = { deltaMode: 1, deltaX: 0, deltaY };
      menu.nativeElement.dispatchEvent(new WheelEvent('wheel', payload));
    }

    function triggerClick(className: string): void {
      const el = fixture.debugElement.query(By.css(className));
      el.nativeElement.dispatchEvent(new MouseEvent('click'));
    }

    function scrollingMenu(): number {
      component.height.menu = component.height.available * 2;
      return component.height.available;
    }

    beforeEach(async(() => {
      appBootstrappedResolve();
    }));

    it('should register a window resize event handler', () => {
      const [eventType] = windowRefMock.nativeWindow.addEventListener.calls.argsFor(0);
      expect(eventType).toBe('resize');
    });

    it('should calculate the height available for the menu', () => {
      expect(component.height.available).toBe(initialWindowHeight - component.height.occupied);
    });

    it('should recalculate the height available for the menu when the window resizes', () => {
      const originallyAvailable = component.height.available;
      triggerResizeEvent(initialWindowHeight + 50);

      expect(component.height.available).toBe(originallyAvailable + 50);
    });

    it('should cache the height of the bottom section of the menu for positioning the down arrow', () => {
      expect(component.height.bottom).toBeDefined();
    });

    it('should cache the amount of vertical pixels not available to the menu for quick recalculations', () => {
      expect(component.height.occupied).toBeDefined();
    });

    it('should move the menu down if the window grows vertically and menu is partially visible at the top', () => {
      component.menuOffsetTop$.next(10);

      triggerResizeEvent(initialWindowHeight - 10);
      expect(component.menuOffsetTop$.value).toBe(10);

      triggerResizeEvent(initialWindowHeight);
      expect(component.menuOffsetTop$.value).toBe(0);
    });

    it('should handle wheel events', () => {
      const upperBound = scrollingMenu();

      // 40px up
      triggerWheelEvent(1);
      expect(component.menuOffsetTop$.value).toBe(40);

      // 80px down (lower out of bounds check)
      triggerWheelEvent(-2);
      expect(component.menuOffsetTop$.value).toBe(0);

      // as far up as possible without going out of bounds
      triggerWheelEvent(upperBound / 40);
      expect(component.menuOffsetTop$.value > (upperBound - 40));

      // over the top (upper out of bounds check)
      triggerWheelEvent(2);
      expect(component.menuOffsetTop$.value).toBe(upperBound);
    });

    it('should handle arrow-down clicks', () => {
      const upperBound = scrollingMenu();

      triggerClick('.arrow-down');
      expect(component.menuOffsetTop$.value).toBe(upperBound);

      // check out-of-bounds
      triggerClick('.arrow-down');
      expect(component.menuOffsetTop$.value).toBe(upperBound);
    });

    it('should handle arrow-up clicks', () => {
      scrollingMenu();

      triggerClick('.arrow-up');
      expect(component.menuOffsetTop$.value).toBe(0);

      // check out-of-bounds
      triggerClick('.arrow-up');
      expect(component.menuOffsetTop$.value).toBe(0);
    });

    it('should not enable arrowDown if available height not yet calculated', () => {
      component.height.available = 0;
      expect(component.moveDownEnabled()).toBe(false);
    });

    it('should enable arrowDown when menu is partially visible at the bottom', () => {
      expect(component.moveDownEnabled()).toBe(false);

      component.height.menu = component.height.available + 10;
      expect(component.moveDownEnabled()).toBe(true);

      component.menuOffsetTop$.next(10);
      expect(component.moveDownEnabled()).toBe(false);
    });

    it('should enable arrowUp when menu is partially visible at the top', () => {
      expect(component.moveUpEnabled()).toBe(false);

      component.height.menu = component.height.available + 10;
      expect(component.moveUpEnabled()).toBe(false);

      component.menuOffsetTop$.next(10);
      expect(component.moveUpEnabled()).toBe(true);
    });

    it('should adjust the transition behavior according to the type of user interaction', () => {
      scrollingMenu();

      // initial load transition
      expect(component.transitionClass$.value).toBe('onload-transition');

      // move menu up and make window larger
      component.menuOffsetTop$.next(10);
      triggerResizeEvent(initialWindowHeight + 10);
      expect(component.transitionClass$.value).toBe('resize-transition');

      triggerClick('.arrow-up');
      expect(component.transitionClass$.value).toBe('click-transition');

      triggerWheelEvent(1);
      expect(component.transitionClass$.value).toBe('wheel-transition');

      triggerClick('.arrow-down');
      expect(component.transitionClass$.value).toBe('click-transition');
    });
  });
});
