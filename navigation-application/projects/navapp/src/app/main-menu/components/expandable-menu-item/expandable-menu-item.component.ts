/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, Input } from '@angular/core';

import { MenuItemContainer } from '../../models';

@Component({
  selector: 'brna-expandable-menu-item',
  templateUrl: 'expandable-menu-item.component.html',
})
export class ExpandableMenuItemComponent {
  @Input()
  config: MenuItemContainer;
}
