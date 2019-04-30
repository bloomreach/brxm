/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, Input } from '@angular/core';

import { MenuItem, MenuItemLink } from '../../models';

@Component({
  selector: 'brna-menu-item',
  templateUrl: 'menu-item.component.html',
  styleUrls: ['menu-item.component.scss'],
})
export class MenuItemComponent {
  @Input()
  config: MenuItem;

  get isContainer(): boolean {
    return this.config && !(this.config as MenuItemLink).id;
  }
}
