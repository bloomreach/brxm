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

import { NavItem, Site } from '@bloomreach/navapp-communication';

export const navigationConfiguration: NavItem[] = [
  {
    id: 'category-ranking',
    appIframeUrl: 'http://localhost:4201',
    appPath: 'category-ranking',
  },
  {
    id: 'all-category-pages',
    appIframeUrl: 'http://localhost:4201',
    appPath: 'all-category-pages',
  },
  {
    id: 'top-opportunities',
    appIframeUrl: 'http://localhost:4201',
    appPath: 'top-opportunities',
  },
  {
    id: 'improve-category-navigation',
    appIframeUrl: 'http://localhost:4201',
    appPath: 'improve-category-navigation',
  },
  {
    id: 'product-a-b-testing',
    appIframeUrl: 'http://localhost:4201',
    appPath: 'product-a-b-testing',
  },
];

export const mockSites: Site[] = [
  {
    id: 0x001,
    name: 'adidas.co.uk',
    subGroups: [
      {
        id: 0x011,
        name: 'UK & Germany',
        subGroups: [
          {
            id: 0x111,
            name: 'Adidas UK',
          },
          {
            id: 0x211,
            name: 'Adidas DE',
          },
        ],
      },
      {
        id: 0x021,
        name: 'Europe',
        subGroups: [
          {
            id: 0x121,
            name: 'Adidas UK',
          },
          {
            id: 0x221,
            name: 'Adidas DE',
          },
          {
            id: 0x321,
            name: 'Adidas FR',
          },
        ],
      },
      {
        id: 0x031,
        name: 'All sites',
        subGroups: [
          {
            id: 0x131,
            name: 'Adidas UK',
          },
          {
            id: 0x231,
            name: 'Adidas DE',
          },
          {
            id: 0x331,
            name: 'Adidas FR',
          },
        ],
      },
    ],
  },
  {
    id: 0x002,
    name: 'An example company that has a a very long name and a subgroup with many items',
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
      {
        id: 0x042,
        name: 'Sub company 004',
      },
      {
        id: 0x052,
        name: 'Sub company 005',
      },
      {
        id: 0x062,
        name: 'Sub company 006',
      },
      {
        id: 0x072,
        name: 'Sub company 007',
      },
      {
        id: 0x082,
        name: 'Sub company 008',
      },
      {
        id: 0x0A2,
        name: 'Sub company 009',
      },
      {
        id: 0x0B2,
        name: 'Sub company 010',
      },
      {
        id: 0x0C2,
        name: 'Sub company 011',
      },
      {
        id: 0x0D2,
        name: 'Sub company 012',
      },
    ],
  },
  {
    id: 0x003,
    name: 'Name\twith\ttabs',
  },
  {
    id: 0x004,
    name: '٩(̾●̮̮̃̾•̃̾)۶',
  },
  {
    id: 0x005,
    name: '°º¤ø,¸¸,ø¤º°`°º¤ø,¸,ø¤°º¤ø,¸¸,ø¤º°`°º¤ø,¸',
  },
  {
    id: 0x006,
    name: '^⨀ᴥ⨀^',
  },
  {
    id: 0x007,
    name: 'Name\twith\ttabs',
  },
  {
    id: 0x008,
    name: 'myapp.com',
    subGroups: [
      {
        id: 0x018,
        name: 'myapp.com.1',
        subGroups: [
          {
            id: 0x118,
            name: 'myapp.com.1.1',
            subGroups: [
              {
                id: 0x1118,
                name: 'myapp.com.1.1.1',
                subGroups: [
                  {
                    id: 0x11118,
                    name: 'myapp.com.1.1.1.1',
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
];
