/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { MenuItem } from '../models';
import { MenuBuilderService } from '../services';

@Component({
  selector: 'brna-main-menu',
  templateUrl: 'main-menu.component.html',
})
export class MainMenuComponent {
  constructor(private menuBuilderService: MenuBuilderService) {}

  get menu(): Observable<MenuItem[]> {
    return this.menuBuilderService.buildMenu();
  }
}
