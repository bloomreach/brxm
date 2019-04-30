/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, Input } from '@angular/core';

import { MenuItem } from '../../models';

@Component({
  selector: 'brna-top-level-menu-item',
  templateUrl: 'top-level-menu-item.component.html',
})
export class TopLevelMenuItemComponent {
  @Input()
  config: MenuItem;
}
