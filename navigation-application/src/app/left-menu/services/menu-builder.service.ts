/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavItem } from '../../models/dto';
import { NavigationConfigurationService } from '../../services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuStructureService } from './menu-structure.service';

@Injectable()
export class MenuBuilderService {
  constructor(
    private navConfigService: NavigationConfigurationService,
    private menuStructureService: MenuStructureService,
  ) {}

  buildMenu(): Observable<MenuItem[]> {
    return this.navConfigService.navigationConfiguration$.pipe(
      map(config => {
        const menu = this.menuStructureService.getMenuStructure();

        this.applyNavigationConfiguration(menu, config);
        return this.filterOutNotConfiguredMenuItems(menu);
      }),
    );
  }

  private applyNavigationConfiguration(menu: MenuItem[], navConfigMap: Map<string, NavItem>): void {
    menu.forEach(item => {
      if (item instanceof MenuItemContainer) {
        this.applyNavigationConfiguration(item.children, navConfigMap);
        return;
      }

      if (navConfigMap.has(item.id)) {
        const config = navConfigMap.get(item.id);

        // One iframe per app is created so app's url can be used as an identifier
        item.appId = config.appIframeUrl;
        item.appPath = config.appPath;
      }
    });
  }

  private filterOutNotConfiguredMenuItems(menu: MenuItem[]): MenuItem[] {
    return menu.filter(item => {
      if (item instanceof MenuItemLink) {
        return !!item.appPath;
      }

      if (item instanceof MenuItemContainer) {
        item.children = this.filterOutNotConfiguredMenuItems(item.children);
        return Array.isArray(item.children) && item.children.length > 0;
      }

      throw new Error('MenuItem has unknown type.');
    });
  }
}
