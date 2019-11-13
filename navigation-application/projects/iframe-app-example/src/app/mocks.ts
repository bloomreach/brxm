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

export const mockNavItems: NavItem[] = [
  {
    id: 'site-search/search-ranking',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'site-search/search-ranking',
  },
  {
    id: 'site-search/ab-testing',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'site-search/ab-testing',
  },
  {
    id: 'site-search/campaigns',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'site-search/campaigns',
  },
  {
    id: 'site-search/assets',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'site-search/assets',
  },
  {
    id: 'site-search/autosuggest-blacklist',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'site-search/autosuggest-blacklist',
  },
  {
    id: 'categories/category-ranking',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'categories/category-ranking',
  },
  {
    id: 'categories/all-category-pages',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'categories/all-category-pages',
  },
  {
    id: 'categories/category-facets-ranking',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'categories/category-facets-ranking',
  },
  {
    id: 'categories/category-facets-ranking',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'categories/category-facets-ranking',
  },
  {
    id: 'seo',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'seo',
  },
  {
    id: 'audiences/merchandising-audiences',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'audiences/merchandising-audiences',
  },
  {
    id: 'insights/opportunities/top-opportunities',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/opportunities/top-opportunities',
  },
  {
    id: 'insights/opportunities/improve-category-navigation',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/opportunities/improve-category-navigation',
  },
  {
    id: 'insights/seo-analytics/overall-performance',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/seo-analytics/overall-performance',
  },
  {
    id: 'insights/seo-analytics/incremental',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/seo-analytics/incremental',
  },
  {
    id: 'insights/seo-analytics/targeted-pages/overview',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/seo-analytics/targeted-pages/overview',
  },
  {
    id: 'insights/seo-analytics/targeted-pages/top-performing-pages',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/seo-analytics/targeted-pages/top-performing-pages',
  },
  {
    id: 'insights/seo-analytics/generated-pages/overview',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/seo-analytics/generated-pages/overview',
  },
  {
    id: 'insights/seo-analytics/generated-pages/top-performing-pages',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/seo-analytics/generated-pages/top-performing-pages',
  },
  {
    id: 'insights/product-a-b-testing',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'insights/product-a-b-testing',
    displayName: 'A/B Testing',
  },
  {
    id: 'not-a-part-of-menu',
    appIframeUrl: 'http://localhost:4201/brsm',
    appPath: 'custom',
  },
];

export const mockSites: Site[] = [
  {
    siteId: -1,
    accountId: 1,
    name: 'www.company.com',
    subGroups: [
      {
        siteId: 1,
        accountId: 1,
        name: 'UK & Germany',
        subGroups: [
          {
            siteId: 2,
            accountId: 1,
            name: 'Office UK',
          },
          {
            siteId: 3,
            accountId: 1,
            name: 'Office DE',
          },
        ],
      },
      {
        siteId: 4,
        accountId: 1,
        name: 'Europe',
        subGroups: [
          {
            siteId: 5,
            accountId: 1,
            name: 'Office UK',
          },
          {
            siteId: 6,
            accountId: 1,
            name: 'Office DE',
          },
          {
            siteId: 7,
            accountId: 1,
            name: 'Office FR',
          },
        ],
      },
      {
        siteId: 8,
        accountId: 1,
        name: 'All sites',
        subGroups: [
          {
            siteId: 9,
            accountId: 1,
            name: 'Office UK',
          },
          {
            siteId: 10,
            accountId: 1,
            name: 'Office DE',
          },
          {
            siteId: 11,
            accountId: 1,
            name: 'Office FR',
          },
        ],
      },
    ],
  },
  {
    siteId: -1,
    accountId: 2,
    name:
      'An example company that has a very long name and a subgroup with many items',
    subGroups: [
      {
        siteId: 12,
        accountId: 2,
        name: 'Sub company 001',
      },
      {
        siteId: 13,
        accountId: 2,
        name: 'Sub company 002',
      },
      {
        siteId: 14,
        accountId: 2,
        name: 'Sub company 003',
      },
      {
        siteId: 15,
        accountId: 2,
        name: 'Sub company 004',
      },
      {
        siteId: 16,
        accountId: 2,
        name: 'Sub company 005',
      },
      {
        siteId: 17,
        accountId: 2,
        name: 'Sub company 006',
      },
      {
        siteId: 18,
        accountId: 2,
        name: 'Sub company 007',
      },
      {
        siteId: 19,
        accountId: 2,
        name: 'Sub company 008',
      },
      {
        siteId: 20,
        accountId: 2,
        name: 'Sub company 009',
      },
      {
        siteId: 21,
        accountId: 2,
        name: 'Sub company 010',
      },
      {
        siteId: 22,
        accountId: 2,
        name: 'Sub company 011',
      },
      {
        siteId: 23,
        accountId: 2,
        name: 'Sub company 012',
      },
    ],
  },
  {
    siteId: -1,
    accountId: 3,
    name: 'Name\twith\ttabs',
  },
  {
    siteId: -1,
    accountId: 4,
    name: '٩(̾●̮̮̃̾•̃̾)۶',
  },
  {
    siteId: -1,
    accountId: 5,
    name: '°º¤ø,¸¸,ø¤º°`°º¤ø,¸,ø¤°º¤ø,¸¸,ø¤º°`°º¤ø,¸',
  },
  {
    siteId: -1,
    accountId: 6,
    name: '^⨀ᴥ⨀^',
  },
  {
    siteId: -1,
    accountId: 7,
    name: 'Name\twith\ttabs',
  },
  {
    siteId: -1,
    accountId: 8,
    name: 'myapp.com',
    subGroups: [
      {
        siteId: 24,
        accountId: 8,
        name: 'myapp.com.1',
        subGroups: [
          {
            siteId: 25,
            accountId: 8,
            name: 'myapp.com.1.1',
            subGroups: [
              {
                siteId: 26,
                accountId: 8,
                name: 'myapp.com.1.1.1',
                subGroups: [
                  {
                    siteId: 27,
                    accountId: 8,
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
