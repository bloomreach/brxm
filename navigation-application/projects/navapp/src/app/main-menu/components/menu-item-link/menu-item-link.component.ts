/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding, Input } from '@angular/core';

import { CommunicationsService } from '../../../communication/services';
import { MenuItemLink } from '../../models';
import { MenuStateService } from '../../services';

@Component({
  selector: 'brna-menu-item-link',
  templateUrl: 'menu-item-link.component.html',
  styleUrls: ['menu-item-link.component.scss'],
})
export class MenuItemLinkComponent {
  @Input()
  config: MenuItemLink;

  @Input()
  @HostBinding('class.active')
  active = false;

  constructor(
    private menuStateService: MenuStateService,
    private communicationsService: CommunicationsService,
  ) {}

  onClick(e: MouseEvent): void {
    e.preventDefault();

    this.menuStateService.setActiveItem(this.config);
    this.communicationsService.navigate(this.config.appId, this.config.appPath);
  }
}
