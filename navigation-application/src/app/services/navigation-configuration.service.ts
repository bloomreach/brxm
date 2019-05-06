/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { navigationConfiguration } from '../mocks';
import { NavItem } from '../models';

@Injectable()
export class NavigationConfigurationService {
  private configStream: Observable<Map<string, NavItem>>;

  get navigationConfiguration$(): Observable<Map<string, NavItem>> {
    if (!this.configStream) {
      this.configStream = this.fetchConfiguration();
    }

    return this.configStream;
  }

  private fetchConfiguration(): Observable<Map<string, NavItem>> {
    return of(navigationConfiguration).pipe(
      map(config => config.reduce(
        (configMap, item) => configMap.set(item.id, item),
        new Map<string, NavItem>(),
      )),
    );
  }
}
