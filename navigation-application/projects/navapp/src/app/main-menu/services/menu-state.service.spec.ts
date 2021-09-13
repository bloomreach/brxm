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

import { fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { first, take } from 'rxjs/operators';

import { NavItemMock } from '../../models/dto/nav-item-dto.mock';
import { NavItemService } from '../../services/nav-item.service';
import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLinkMock } from '../models/menu-item-link.mock';
import { MenuItem } from '../models/menu-item.model';

import { MenuBuilderService } from './menu-builder.service';
import { MenuStateService } from './menu-state.service';
import { MenuStructureService } from './menu-structure.service';

describe('MenuStateService', () => {
  let service: MenuStateService;

  const navItemService = jasmine.createSpyObj('MenuStructureService', {
    findNavItem: {
      id: 'nav-item-2',
      appIframeUrl: 'http://domain.com/iframe1/url',
      appPath: 'app/path/to/page1',
    },
  });

  const navItemsMock = [
    new NavItemMock({ id: '1' }),
    new NavItemMock({ id: '2' }),
  ];

  const builtMenuMock = [
    new MenuItemContainer(
      'menu item 1',
      [
        new MenuItemLinkMock({
          id: 'nav-item-1',
          caption: 'menu link subitem 1',
          navItem: new NavItemMock({
            id: 'nav-item-1',
            appIframeUrl: 'http://domain.com/iframe1/url',
            appPath: 'app/path/to/home',
          }),
        }),
        new MenuItemLinkMock({
          id: 'nav-item-2',
          caption: 'menu link subitem 2',
          navItem: new NavItemMock({
            id: 'nav-item-2',
            appIframeUrl: 'http://domain.com/iframe1/url',
            appPath: 'app/path/to/page1',
          }),
        }),
      ],
    ),
    new MenuItemLinkMock({
      id: 'nav-item-3',
      caption: 'menu link 1',
      navItem: new NavItemMock({
        id: 'nav-item-3',
        appIframeUrl: 'http://domain.com/iframe2/url',
        appPath: 'app/path/to/home',
      }),
    }),
  ] as any[];

  const menuBuilderServiceMock = jasmine.createSpyObj('MenuBuilderService', {
    buildMenu: builtMenuMock,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MenuStateService,
        { provide: NavItemService, useValue: navItemService },
        { provide: MenuBuilderService, useValue: menuBuilderServiceMock },
      ],
    });

    service = TestBed.inject(MenuStateService);

    service.init(navItemsMock);
  });

  it('should build the menu based on the provided nav items', () => {
    expect(menuBuilderServiceMock.buildMenu).toHaveBeenCalledWith(navItemsMock);
  });

  it('should emit the built menu', async () => {
    const expected = builtMenuMock;

    const actual = await service.menu$.pipe(
      take(1),
    ).toPromise();

    expect(actual).toEqual(expected);
  });

  it('should return the found home menu item', () => {
    const expected = builtMenuMock[0].children[0];

    const actual = service.currentHomeMenuItem;

    expect(actual).toEqual(expected);
  });

  it('should update the active path when the menu item is activated', fakeAsync(() => {
    const expected = [
      builtMenuMock[0],
      builtMenuMock[0].children[1],
    ];

    let actual: any;

    service.activePath$.subscribe(x => actual = x);

    service.activateMenuItem('http://domain.com/iframe1/url', 'app/path/to/page1');

    tick();

    expect(actual).toEqual(expected);
  }));

  it('should toggle the menu', () => {
    expect(service.isMenuCollapsed).toBeTruthy();

    service.toggle();

    expect(service.isMenuCollapsed).toBeFalsy();

    service.toggle();

    expect(service.isMenuCollapsed).toBeTruthy();
  });

  it('should open the drawer', () => {
    service.openDrawer(builtMenuMock[0]);

    expect(service.isDrawerOpened).toBeTruthy();
  });

  it('should close the drawer', () => {
    service.openDrawer(builtMenuMock[0]);

    expect(service.isDrawerOpened).toBeTruthy();

    service.closeDrawer();

    expect(service.isDrawerOpened).toBeFalsy();
  });

  it('should return the drawer menu item', () => {
    const expected = builtMenuMock[0];

    service.openDrawer(builtMenuMock[0]);

    expect(service.drawerMenuItem).toEqual(expected);
  });

  describe('when there is an active menu item', () => {
    beforeEach(waitForAsync(() => {
      service.activateMenuItem('http://domain.com/iframe1/url', 'app/path/to/page1');
    }));

    it('should check for activeness positively the root active element', () => {
      const actual = service.isMenuItemHighlighted(builtMenuMock[0]);

      expect(actual).toBeTruthy();
    });

    it('should check for activeness positively the child active element', () => {
      const actual = service.isMenuItemHighlighted(builtMenuMock[0].children[1]);

      expect(actual).toBeTruthy();
    });

    it('should check for activeness negatively an inactive menu item', () => {
      const actual = service.isMenuItemHighlighted(builtMenuMock[1]);

      expect(actual).toBeFalsy();
    });

    it('should deactivate the currently active menu item', () => {
      service.deactivateMenuItem();

      const actual = service.isMenuItemHighlighted(builtMenuMock[0]);

      expect(actual).toBeFalsy();
    });
  });

  describe('when active menu item is failed', () => {
    beforeEach(waitForAsync(() => {
      service.activateMenuItem('http://domain.com/iframe1/url', 'app/path/to/page1');
      service.markMenuItemAsFailed(new NavItemMock({
        id: 'nav-item-2',
        appIframeUrl: 'http://domain.com/iframe1/url',
        appPath: 'app/path/to/page1',
      }));
    }));

    it('should do nothing if navigation item is undefined', () => {
      const navItem = undefined;

      expect(() => service.markMenuItemAsFailed(navItem)).not.toThrow();
    });

    it('should mark menu item as failed', () => {
      const failed = service.isMenuItemFailed(builtMenuMock[0].children[1]);

      expect(failed).toBeTrue();
    });

    it('should clean failed menu item when activate current menu item', () => {
      service.activateMenuItem('http://domain.com/iframe1/url', 'app/path/to/page1');

      const failed = service.isMenuItemFailed(builtMenuMock[0].children[1]);

      expect(failed).toBeFalse();
    });

  });
});
