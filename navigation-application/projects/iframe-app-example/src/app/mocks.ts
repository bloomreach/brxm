import { NavItem } from '@bloomreach/navapp-communication';

export const navigationConfiguration: NavItem[] = [
  {
    id: 'experience-manager',
    appIframeUrl: 'localhost:4201/brxm',
    appPath: 'experience-manager',
  },
  {
    id: 'projects',
    appIframeUrl: 'localhost:4201/brxm',
    appPath: 'projects',
  },
  {
    id: 'category-ranking',
    appIframeUrl: 'localhost:4201/brsm/category-ranking',
    appPath: 'category-ranking',
  },
  {
    id: 'all-category-pages',
    appIframeUrl: 'localhost:4201/brsm/all-category-pages',
    appPath: 'all-category-pages',
  },
  {
    id: 'top-opportunities',
    appIframeUrl: 'localhost:4201/brsm/opportunities',
    appPath: 'top-opportunities',
  },
  {
    id: 'improve-category-navigation',
    appIframeUrl: 'localhost:4201/brsm/improve-category-navigation',
    appPath: 'improve-category-navigation',
  },
  {
    id: 'product-a-b-testing',
    appIframeUrl: 'localhost:4201/brsm/product-a-b-testing',
    appPath: 'product-a-b-testing',
  },
];
