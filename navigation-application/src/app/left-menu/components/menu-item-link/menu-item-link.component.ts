/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, Input } from '@angular/core';

import { MenuItemLink } from '../../models';
import { CommunicationsService } from '../../../communication/services';

@Component({
  selector: 'brna-menu-item-link',
  templateUrl: 'menu-item-link.component.html',
})
export class MenuItemLinkComponent {
  @Input()
  config: MenuItemLink;

  constructor(private communicationsService: CommunicationsService) {}

  onClick(e: MouseEvent): void {
    e.preventDefault();

    this.communicationsService.navigate(this.config.appId, this.config.appPath);
  }
}
