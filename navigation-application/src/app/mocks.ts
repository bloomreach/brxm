/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { NavItem } from './models';

export const navigationConfiguration: NavItem[] = [
  {
    id: 'experience-manager',
    appIframeUrl: 'brxm',
    appPath: 'experience-manager',
  },
  {
    id: 'projects',
    appIframeUrl: 'brxm',
    appPath: 'projects',
  },
  {
    id: 'category-ranking',
    appIframeUrl: 'brsm/category-ranking',
    appPath: 'category-ranking',
  },
  {
    id: 'all-category-pages',
    appIframeUrl: 'brsm/all-category-pages',
    appPath: 'all-category-pages',
  },
  {
    id: 'top-opportunities',
    appIframeUrl: 'brsm/opportunities',
    appPath: 'top-opportunities',
  },
  {
    id: 'improve-category-navigation',
    appIframeUrl: 'brsm/improve-category-navigation',
    appPath: 'improve-category-navigation',
  },
  {
    id: 'product-a-b-testing',
    appIframeUrl: 'brsm/product-a-b-testing',
    appPath: 'product-a-b-testing',
  },
];
