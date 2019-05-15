/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding, OnInit } from '@angular/core';

import { CommunicationsService } from '../../communication/services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';
import { MenuStateService } from '../services';

@Component({
  selector: 'brna-left-menu',
  templateUrl: 'left-menu.component.html',
  styleUrls: ['left-menu.component.scss'],
  providers: [
    MenuStateService,
  ],
})
export class LeftMenuComponent implements OnInit {
  collapsed = true;
  menu: MenuItem[];

  constructor(
    private menuStateService: MenuStateService,
    private communicationsService: CommunicationsService,
  ) {}

  get isDrawerOpen(): boolean {
    return this.menuStateService.isDrawerOpened;
  }

  get drawerMenuItem(): MenuItemContainer {
    return this.menuStateService.drawerMenuItem;
  }

  @HostBinding('class.collapsed')
  get isCollapsed(): boolean {
    return this.collapsed;
  }

  ngOnInit(): void {
    this.menu = this.menuStateService.menu;
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }

  onMenuItemClick(event: MouseEvent, item: MenuItem): void {
    event.stopImmediatePropagation();

    if (item instanceof MenuItemLink) {
      this.menuStateService.setActiveItem(item);
      this.communicationsService.navigate(item.appId, item.appPath);
      return;
    }

    if (item instanceof MenuItemContainer) {
      this.menuStateService.openDrawer(item);
    }
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }
}
