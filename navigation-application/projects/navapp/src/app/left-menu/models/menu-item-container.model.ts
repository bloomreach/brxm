/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { MenuItem } from './menu-item';

export class MenuItemContainer {
  constructor(
    public caption: string,
    public children: MenuItem[],
  ) {}
}
