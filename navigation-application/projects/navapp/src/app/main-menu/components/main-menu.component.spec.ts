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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, Subscription } from 'rxjs';

import { NavItemMock } from '../../models/nav-item.mock';
import { NavItem } from '../../models/nav-item.model';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { NavigationService } from '../../services/navigation.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { WindowRef } from '../../shared/services/window-ref.service';
import { MenuItemContainerMock } from '../models/menu-item-container.mock';
import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLinkMock } from '../models/menu-item-link.mock';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';
import { MenuStateService } from '../services/menu-state.service';

import { MainMenuComponent } from './main-menu.component';

describe('MainMenuComponent', () => {
  let component: MainMenuComponent;
  let fixture: ComponentFixture<MainMenuComponent>;
  let de: DebugElement;

  const menuMock = [
    new MenuItemLinkMock({
      id: 'item1',
      navItem: new NavItemMock({
        id: 'someId',
        appIframeUrl: 'homeAppUrl',
        appPath: 'homeAppPath',
      }),
    }),
    new MenuItemLinkMock({
      id: 'item2',
    }),
  ];

  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;
  let qaHelperServiceMock: jasmine.SpyObj<QaHelperService>;
  let busyIndicatorServiceMock: jasmine.SpyObj<BusyIndicatorService>;
  let navigationServiceMock: jasmine.SpyObj<NavigationService>;
  let windowRefMock: any;
  const initialWindowHeight = 800;

  beforeEach(async(() => {
    menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
      'toggle',
      'openDrawer',
      'closeDrawer',
      'isMenuItemHighlighted',
      'activateMenuItem',
    ]);
    (menuStateServiceMock as any).menu = menuMock;
    (menuStateServiceMock as any).isDrawerOpened = false;

    qaHelperServiceMock = jasmine.createSpyObj('QaHelperService', [
      'getMenuItemClass',
    ]);

    busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    navigationServiceMock = jasmine.createSpyObj('NavigationService', [
      'navigateByNavItem',
    ]);

    windowRefMock = {
      nativeWindow: {
        innerHeight: initialWindowHeight,
        addEventListener: jasmine.createSpy('addEventListener'),
        removeEventListener: jasmine.createSpy('removeEventListener'),
      },
    };

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      declarations: [MainMenuComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: QaHelperService, useValue: qaHelperServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: WindowRef, useValue: windowRefMock },
      ],
    }).createComponent(MainMenuComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    menuStateServiceMock = TestBed.get(MenuStateService);
    busyIndicatorServiceMock = TestBed.get(BusyIndicatorService);

    component.ngOnInit();

    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show menu items', () => {
    const menuItems = de.queryAll(By.css('brna-top-level-menu-item'));

    expect(menuItems.length).toBe(4);
  });

  it('should the busy indicator', fakeAsync(() => {
    (busyIndicatorServiceMock as any).isVisible = true;

    fixture.detectChanges();

    tick();

    const drawerEl = de.query(By.css('mat-progress-bar'));

    expect(drawerEl).not.toBeNull();
  }));

  it('should provide the collapsed state', () => {
    (menuStateServiceMock as any).isMenuCollapsed = true;

    expect(component.collapsed).toBeTruthy();
  });

  it('should set .collapsed class', fakeAsync(() => {
    (menuStateServiceMock as any).isMenuCollapsed = true;

    fixture.detectChanges();

    tick();

    expect(de.classes.collapsed).toBeTruthy();
  }));

  it('should toggle expanded/collapsed state', () => {
    const triggerEl = de.query(By.css('.trigger'));

    triggerEl.triggerEventHandler('click', {});

    expect(menuStateServiceMock.toggle).toHaveBeenCalled();
  });

  it('should open the drawer', fakeAsync(() => {
    (menuStateServiceMock as any).isDrawerOpened = true;

    fixture.detectChanges();

    tick();

    const drawerEl = de.query(By.css('brna-menu-drawer'));

    expect(component.isDrawerOpen).toBeTruthy();
    expect(drawerEl).not.toBeNull();
  }));

  it('should provide a drawer menu item', () => {
    const expected: MenuItemContainer = {} as any;

    (menuStateServiceMock as any).drawerMenuItem = expected;

    expect(component.drawerMenuItem).toBe(expected);
  });

  describe('when a menu item is clicked', () => {
    beforeEach(() => {
      const menuItem: MenuItem = {} as any;

      component.onMenuItemClick(menuItem);
    });

    it('should close help toolbar drawer', () => {
      const helpToolbarEl = de.query(By.css('brna-help-toolbar-drawer'));

      expect(component.isHelpToolbarOpened).toBeFalsy();
      expect(helpToolbarEl).toBeNull();
    });

    it('should close user toolbar drawer', () => {
      const userToolbarEl = de.query(By.css('brna-user-toolbar-drawer'));

      expect(component.isUserToolbarOpened).toBeFalsy();
      expect(userToolbarEl).toBeNull();
    });

    describe('and menu item is MenuItemLink instance', () => {
      const navItem = new NavItemMock();

      beforeEach(() => {
        component.onMenuItemClick(new MenuItemLinkMock({ navItem }));
      });

      it('should navigate to the nav item contained', () => {
        expect(navigationServiceMock.navigateByNavItem).toHaveBeenCalledWith(navItem, NavigationTrigger.Menu);
      });
    });

    describe('and menu item is MenuItemContainer instance', () => {
      const menuItem = new MenuItemContainerMock();

      beforeEach(() => {
        component.onMenuItemClick(menuItem);
      });

      it('should open the drawer', () => {
        expect(menuStateServiceMock.openDrawer).toHaveBeenCalledWith(menuItem);
      });
    });
  });

  describe('when help menu item is clicked', () => {
    beforeEach(async(() => {
      component.onHelpMenuItemClick();

      fixture.detectChanges();
    }));

    it('should close the drawer', () => {
      expect(menuStateServiceMock.closeDrawer).toHaveBeenCalled();
    });

    it('should close user toolbar drawer', () => {
      const userToolbarEl = de.query(By.css('brna-user-toolbar-drawer'));

      expect(component.isUserToolbarOpened).toBeFalsy();
      expect(userToolbarEl).toBeNull();
    });

    it('should open help toolbar drawer', () => {
      const helpToolbarEl = de.query(By.css('brna-help-toolbar-drawer'));

      expect(component.isHelpToolbarOpened).toBeTruthy();
      expect(helpToolbarEl).not.toBeNull();
    });
  });

  describe('when user menu item is clicked', () => {
    beforeEach(async(() => {
      component.onUserMenuItemClick();

      fixture.detectChanges();
    }));

    it('should close the drawer', () => {
      expect(menuStateServiceMock.closeDrawer).toHaveBeenCalled();
    });

    it('should close help toolbar drawer', () => {
      const helpToolbarEl = de.query(By.css('brna-help-toolbar-drawer'));

      expect(component.isHelpToolbarOpened).toBeFalsy();
      expect(helpToolbarEl).toBeNull();
    });

    it('should open user toolbar drawer', () => {
      const userToolbarEl = de.query(By.css('brna-user-toolbar-drawer'));

      expect(component.isUserToolbarOpened).toBeTruthy();
      expect(userToolbarEl).not.toBeNull();
    });
  });

  describe('isHighlighted', () => {
    const menuItem = new MenuItemLinkMock();

    beforeEach(() => {
      menuStateServiceMock.isMenuItemHighlighted.and.returnValue(true);
    });

    it('should forward the provided menu item to the service', () => {
      component.isMenuItemHighlighted(menuItem);

      expect(menuStateServiceMock.isMenuItemHighlighted).toHaveBeenCalledWith(menuItem);
    });

    it('should return the value return by the service', () => {
      const actual = component.isMenuItemHighlighted(menuItem);

      expect(actual).toBeTruthy();
    });
  });

  describe('isMenuItemDisabled', () => {
    describe('when MenuItemLink instance is provided', () => {
      const navItemActiveSubject = new Subject<boolean>();
      let subscription: Subscription;

      let navItem: NavItem;
      let menuItemLink: MenuItemLink;
      let activeSnapshot: boolean;

      beforeEach(() => {
        activeSnapshot = undefined;
        navItem = new NavItemMock({}, navItemActiveSubject);
        menuItemLink = new MenuItemLinkMock({ navItem });

        subscription = component
          .isMenuItemDisabled(menuItemLink)
          .subscribe(active => activeSnapshot = active);
      });

      afterEach(() => {
        subscription.unsubscribe();
      });

      it('should return "true" initially', () => {
        expect(activeSnapshot).toBeTruthy();
      });

      it('should return "false" if a nav item is active', () => {
        navItemActiveSubject.next(true);

        expect(activeSnapshot).toBeFalsy();
      });
    });

    describe('when MenuItemContainer instance is provided', () => {
      let subscription: Subscription;
      let activeSnapshot: boolean;

      beforeEach(() => {
        subscription = component
          .isMenuItemDisabled(new MenuItemContainerMock())
          .subscribe(active => activeSnapshot = active);
      });

      afterEach(() => {
        subscription.unsubscribe();
      });

      it('should return "false"', () => {
        expect(activeSnapshot).toBeFalsy();
      });
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
