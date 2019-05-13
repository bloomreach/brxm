/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding } from '@angular/core';
import { Observable } from 'rxjs';

import { MenuItem } from '../models';
import { MenuBuilderService } from '../services';

@Component({
  selector: 'brna-left-menu',
  templateUrl: 'left-menu.component.html',
  styleUrls: ['left-menu.component.scss'],
})
export class LeftMenuComponent {
  collapsed = true;

  constructor(private menuBuilderService: MenuBuilderService) {}

  get menu(): Observable<MenuItem[]> {
    return this.menuBuilderService.buildMenu();
  }

  @HostBinding('class.collapsed')
  get isCollapsed(): boolean {
    return this.collapsed;
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }
}
