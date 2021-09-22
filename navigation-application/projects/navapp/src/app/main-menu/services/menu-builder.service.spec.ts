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
import { NavItem } from '@bloomreach/navapp-communication';

import { NavItemMock } from '../../models/dto/nav-item-dto.mock';
import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';

import { MenuBuilderService } from './menu-builder.service';
import { MenuStructureService } from './menu-structure.service';

describe('MenuBuilderService', () => {
  let menuBuilderService: MenuBuilderService;

  const createMenuItemLink = (id: string, caption: string, navItem: NavItem, icon?: string) => {
    return Object.assign(
      new MenuItemLink(id, caption, icon),
      { navItem },
    );
  };

  const getMenuStructure = () => [
    new MenuItemLink('testNavItemId', 'Root menu item 1'),
    new MenuItemContainer(
      'Container menu item 1',
      [
        new MenuItemLink('subitem1', 'Sub item 1'),
        new MenuItemLink('subitem2', 'Sub item 2'),
        new MenuItemLink('subitem3', 'Sub item 3'),
        new MenuItemContainer(
          'Sub container menu item 1',
          [
            new MenuItemLink('subsubitem1', 'Sub item 1'),
            new MenuItemLink('subsubitem2', 'Sub item 2'),
            new MenuItemLink('subsubitem3', 'Sub item 3'),
          ],
        ),
      ],
    ),
  ];

  const menuStructureServiceMock = jasmine.createSpyObj('MenuStructureService', [
    'getMenuStructure',
    'addExtension',
  ]);

  beforeEach(() => {
    menuStructureServiceMock.getMenuStructure.and.returnValue(getMenuStructure());

    TestBed.configureTestingModule({
      providers: [
        MenuBuilderService,
        { provide: MenuStructureService, useValue: menuStructureServiceMock },
      ],
    });

    menuBuilderService = TestBed.inject(MenuBuilderService);
  });

  it('should get the filtered menu populated with app paths', () => {
    const navItem = new NavItemMock({ id: 'testNavItemId' });
    const expected = [
      createMenuItemLink('testNavItemId', 'Root menu item 1', navItem),
    ];

    const navItemsMock = [
      navItem,
    ];
    const actual = menuBuilderService.buildMenu(navItemsMock);

    expect(actual).toEqual(expected);
  });

  it('should get the filtered and reduced menu populated with app paths', () => {
    const navItemsMock = [
      new NavItemMock({ id: 'testNavItemId' }),
      new NavItemMock({ id: 'subsubitem3' }),
    ];

    const expected = [
      createMenuItemLink('testNavItemId', 'Root menu item 1', navItemsMock[0]),
      createMenuItemLink('subsubitem3', 'Sub item 3', navItemsMock[1]),
    ];

    const actual = menuBuilderService.buildMenu(navItemsMock);

    expect(actual).toEqual(expected);
  });

  it('should add a nav item as an extension menu item when displayName is set', () => {
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

    const expected = [
      createMenuItemLink('testNavItemId', 'Root menu item 1', navItemsMock[0]),
    ];

    const actual = menuBuilderService.buildMenu(navItemsMock);

    expect(actual).toEqual(expected);
  });
});
