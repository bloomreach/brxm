/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { CommunicationsService } from '../../communication/services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';
import { MenuBuilderService } from '../services';

@Component({
  selector: 'brna-left-menu',
  templateUrl: 'left-menu.component.html',
  styleUrls: ['left-menu.component.scss'],
})
export class LeftMenuComponent implements OnInit {
  collapsed = true;
  menu: Observable<MenuItem[]>;

  private activeMenuItem: MenuItem;

  constructor(
    private menuBuilderService: MenuBuilderService,
    private communicationsService: CommunicationsService,
  ) {}

  get isDrawerOpen(): boolean {
    return this.activeMenuItem instanceof MenuItemContainer;
  }

  @HostBinding('class.collapsed')
  get isCollapsed(): boolean {
    return this.collapsed;
  }

  ngOnInit(): void {
    this.menu = this.menuBuilderService.buildMenu();
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }

  onMenuItemClick(item: MenuItem): void {
    this.activeMenuItem = item;

    if (item instanceof MenuItemLink) {
      this.communicationsService.navigate(item.appId, item.appPath);
    }
  }

  isMenuItemActive(item: MenuItem): boolean {
    return item === this.activeMenuItem;
  }
}
