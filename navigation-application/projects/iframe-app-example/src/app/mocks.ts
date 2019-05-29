import { NavItem } from '@bloomreach/navapp-communication';

export const navigationConfiguration: NavItem[] = [
  {
    id: 'category-ranking',
    appIframeUrl: 'http://localhost:4201/?parent=http://localhost:8080',
    appPath: 'category-ranking',
  },
  {
    id: 'all-category-pages',
    appIframeUrl: 'http://localhost:4201/?parent=http://localhost:8080',
    appPath: 'all-category-pages',
  },
  {
    id: 'top-opportunities',
    appIframeUrl: 'http://localhost:4201/?parent=http://localhost:8080',
    appPath: 'top-opportunities',
  },
  {
    id: 'improve-category-navigation',
    appIframeUrl: 'http://localhost:4201/?parent=http://localhost:8080',
    appPath: 'improve-category-navigation',
  },
  {
    id: 'product-a-b-testing',
    appIframeUrl: 'http://localhost:4201/?parent=http://localhost:8080',
    appPath: 'product-a-b-testing',
  },
];
