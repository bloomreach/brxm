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
import { MenuItemLink } from '../models/menu-item-link.model';

import { MenuBuilderService } from './menu-builder.service';
import { MenuStructureService } from './menu-structure.service';

describe('MenuBuilderService', () => {
  let menuBuilderService: MenuBuilderService;

  const menuStructureServiceMock = jasmine.createSpyObj(
    'MenuStructureService',
    ['getMenuStructure', 'addExtension'],
  );

  const menuItemsMock = [
    new MenuItemLinkMock({ id: 'testNavItemId' }),
    new MenuItemContainerMock({
      caption: 'submenu',
      children: [
        new MenuItemLinkMock({ id: 'subitem1' }),
        new MenuItemLinkMock({ id: 'subitem2' }),
        new MenuItemLinkMock({ id: 'subitem3' }),
      ],
    }),
  ];

  const navItemsMock = [new NavItemMock()];

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
    const actual = menuBuilderService.buildMenu(navItemsMock);

    expect((actual[0] as MenuItemLink).navItem).toEqual(navItemsMock[0]);
  });
});
