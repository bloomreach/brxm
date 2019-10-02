/*!
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

import { NavItemMock } from '../models/dto/nav-item.mock';

import { NavItemService } from './nav-item.service';

describe('NavItemService', () => {
  let navItemService: NavItemService;

  const appIframeUrl = 'https://test.url';
  const mockNavItems = [
    new NavItemMock({
      appPath: 'test1',
      appIframeUrl,
    }),
    new NavItemMock({
      appPath: 'test2',
      appIframeUrl,
    }),
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NavItemService,
      ],
    });

    navItemService = TestBed.get(NavItemService);
    navItemService.navItems = mockNavItems;
  });

  it('should find navitem by iframe url and path', () => {
    expect(navItemService.findNavItem(appIframeUrl, 'test2')).toEqual(mockNavItems[1]);
  });
});
