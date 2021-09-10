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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { TranslateModule } from '@ngx-translate/core';
import { NEVER, of } from 'rxjs';

import { NavItemMock } from '../../models/dto/nav-item-dto.mock';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { NavigationService } from '../../services/navigation.service';
import { QaHelperService } from '../../services/qa-helper.service';
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

  beforeEach(waitForAsync(() => {
    menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
      'toggle',
      'openDrawer',
      'closeDrawer',
      'isMenuItemHighlighted',
      'activateMenuItem',
      'isMenuItemFailed',
    ]);
    (menuStateServiceMock as any).menu$ = of(menuMock);
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
      ],
    }).createComponent(MainMenuComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    menuStateServiceMock = TestBed.inject(MenuStateService) as jasmine.SpyObj<MenuStateService>;
    busyIndicatorServiceMock = TestBed.inject(BusyIndicatorService) as jasmine.SpyObj<BusyIndicatorService>;

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
    beforeEach(waitForAsync(() => {
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
    beforeEach(waitForAsync(() => {
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
});
