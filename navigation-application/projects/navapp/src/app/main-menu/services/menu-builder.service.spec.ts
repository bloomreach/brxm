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

import { TestBed } from '@angular/core/testing';

import { NavItemMock } from '../../models/dto/nav-item.mock';
import { MenuItemContainerMock } from '../models/menu-item-container.mock';
import { MenuItemLinkMock } from '../models/menu-item-link.mock';

import { MenuBuilderService } from './menu-builder.service';
import { MenuStructureService } from './menu-structure.service';

describe('MenuBuilderService', () => {
  let menuBuilderService: MenuBuilderService;

  const menuStructureServiceMock = jasmine.createSpyObj(
    'MenuStructureService',
    ['getMenuStructure', 'addExtension'],
  );

  const menuItemsMock = [
    new MenuItemLinkMock({ id: 'testNavItemId', caption: 'Root menu item 1', navItem: null }),
    new MenuItemContainerMock({
      caption: 'submenu',
      children: [
        new MenuItemLinkMock({ id: 'subitem1', caption: 'Sub item 1', navItem: null }),
        new MenuItemLinkMock({ id: 'subitem2', caption: 'Sub item 2', navItem: null }),
        new MenuItemLinkMock({ id: 'subitem3', caption: 'Sub item 3', navItem: null }),
      ],
    }),
  ];

  menuStructureServiceMock.getMenuStructure.and.returnValue(menuItemsMock);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MenuBuilderService,
        { provide: MenuStructureService, useValue: menuStructureServiceMock },
      ],
    });

    menuBuilderService = TestBed.get(MenuBuilderService);
  });

  it('should get the filtered menu populated with app paths', () => {
    const expected = [
      new MenuItemLinkMock({ id: 'testNavItemId', caption: 'Root menu item 1', navItem: new NavItemMock() }),
    ];

    const navItemsMock = [new NavItemMock()];
    const actual = menuBuilderService.buildMenu(navItemsMock);

    expect(actual).toEqual(expected);
  });

  it('should add a nav item as an extension menu item when displayName is set', () => {
    const expected = [
      new MenuItemLinkMock({ id: 'testNavItemId', caption: 'Root menu item 1', navItem: new NavItemMock() }),
    ];

    const navItemsMock = [
      new NavItemMock(),
      new NavItemMock({
        id: 'unknown1',
        displayName: undefined,
      }),
      new NavItemMock({
        id: 'unknown2',
        displayName: 'Some display name',
      }),
    ];

    const actual = menuBuilderService.buildMenu(navItemsMock);

    expect(actual).toEqual(expected);
  });
});
