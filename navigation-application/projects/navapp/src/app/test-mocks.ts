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

import { MenuItemContainer } from './main-menu/models/menu-item-container.model';
import { MenuItemLink } from './main-menu/models/menu-item-link.model';
import { MenuItem } from './main-menu/models/menu-item.model';
import { NavItem } from './models/dto/nav-item.dto';
import { Site } from './models/dto/site.dto';
import { GlobalSettings } from './models/global-settings.model';

const item1 = new MenuItemLink('item1', 'Item 1');

const item2 = new MenuItemContainer('Item 2', [
  new MenuItemContainer('Sub item 1', [
    new MenuItemLink('subsubitem1', 'Sub sub item 1'),
    new MenuItemLink('subsubitem2', 'Sub sub item 2'),
    new MenuItemLink('subsubitem3', 'Sub sub item 3'),
  ]),
  new MenuItemLink('subitem2', 'Sub item 2'),
]);

export const menuStructureMock: MenuItem[] = [item1, item2];

export const navConfig: NavItem[] = [
  {
    id: 'subsubitem1',
    appIframeUrl: 'iframe1-url',
    appPath: 'app-path-for-sub-sub-item1',
  },
  {
    id: 'subitem2',
    appIframeUrl: 'iframe2-url',
    appPath: 'app-path-for-sub-item2',
  },
];

export const globalSettings: GlobalSettings = {

  userSettings: {
    userName: 'Frank Zappa',
    language: 'en',
    timeZone: 'Europe/Amsterdam',
  },
  appSettings: {
    navAppLocation: 'http://localhost:4200',
    contextPath: '/cms',
    navConfigResources: [
      {
        resourceType: 'IFRAME',
        url: 'testIFRAMEurl',
      },
      {
        resourceType: 'REST',
        url: 'testRESTurl',
      },
    ],
    loginResources: [],
    logoutResources: [],
  },
};

export const mockSites: Site[] = [
  {
    id: 1,
    name: 'www.company.com',
    subGroups: [
      {
        id: 4,
        name: 'UK & Germany',
        subGroups: [
          {
            id: 0x111,
            name: 'Office UK',
          },
        ],
      },
    ],
  },
  {
    id: 2,
    name:
      'An example company that has a very long name and a subgroup with many items',
    subGroups: [
      {
        id: 0x012,
        name: 'Sub company 001',
      },
      {
        id: 0x022,
        name: 'Sub company 002',
      },
      {
        id: 0x032,
        name: 'Sub company 003',
      },
    ],
  },
];
