/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { NavItem } from '../models';

@Injectable()
export class NavigationConfigurationService {
  private navigationConfiguration = new BehaviorSubject<Map<string, NavItem>>(
    new Map(),
  );

  get navigationConfiguration$(): Observable<Map<string, NavItem>> {
    return this.navigationConfiguration.asObservable();
  }

  setNavigationConfiguration(navItems: NavItem[]): void {
    const navItemMap = navItems.reduce(
      (configMap, item) => configMap.set(item.id, item),
      new Map<string, NavItem>(),
    );

    this.navigationConfiguration.next(navItemMap);
  }
}
