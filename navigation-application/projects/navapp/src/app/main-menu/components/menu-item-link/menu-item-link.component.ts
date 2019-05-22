/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding, Input } from '@angular/core';

import { QaHelperService } from '../../../services';
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
    private qaHelperService: QaHelperService,
  ) {}

  onClick(e: MouseEvent): void {
    e.preventDefault();

    this.menuStateService.setActiveItem(this.config);
  }

  getQaClass(item: MenuItemLink): string {
    return this.qaHelperService.getMenuItemClass(item);
  }
}
