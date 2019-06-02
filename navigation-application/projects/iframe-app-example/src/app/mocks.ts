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

import { NavItem } from '@bloomreach/navapp-communication';

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
