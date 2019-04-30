/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { MenuItem, MenuItemContainer, MenuItemLink } from './left-menu/models';
import { NavItem } from './models';

const item1 = new MenuItemLink(
  'item1',
  'Item 1',
);

const item2 = new MenuItemContainer(
  'Item 2',
  [
    new MenuItemContainer(
      'Sub item 1',
      [
        new MenuItemLink(
          'subsubitem1',
          'Sub sub item 1',
        ),
        new MenuItemLink(
          'subsubitem2',
          'Sub sub item 2',
        ),
        new MenuItemLink(
          'subsubitem3',
          'Sub sub item 3',
        ),
      ],
    ),
    new MenuItemLink(
      'subitem2',
      'Sub item 2',
    ),
  ],
);

export const menuStructureMock: MenuItem[] = [
  item1,
  item2,
];

const navConfig: NavItem[] = [
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

export const navigationConfigurationMapMock: Map<string, NavItem> = navConfig.reduce(
  (map, item) => map.set(item.id, item),
  new Map(),
);
