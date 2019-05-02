/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, Input } from '@angular/core';

import { MenuItemLink } from '../../models';

@Component({
  selector: 'brna-menu-item-link',
  templateUrl: 'menu-item-link.component.html',
})
export class MenuItemLinkComponent {
  @Input()
  config: MenuItemLink;

  onClick(e: MouseEvent): void {
    e.preventDefault();

    alert(`Should navigate to: ${this.config.appPath}`);
  }
}
