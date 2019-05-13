/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding, Input } from '@angular/core';

import { MenuItem } from '../../models';

@Component({
  selector: 'brna-top-level-menu-item',
  templateUrl: 'top-level-menu-item.component.html',
  styleUrls: ['top-level-menu-item.component.scss'],
})
export class TopLevelMenuItemComponent {
  @Input()
  config: MenuItem;

  @Input()
  @HostBinding('class.collapsed')
  collapsed = true;
}
