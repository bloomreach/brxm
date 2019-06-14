/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Injectable } from '@angular/core';
import { NavLocation, ParentApi } from '@bloomreach/navapp-communication';
import { Observable } from 'rxjs';
import { first, mergeAll } from 'rxjs/operators';

import { MenuItem, MenuItemLink } from '../main-menu/models';
import { MenuStateService } from '../main-menu/services';

import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class AppToNavAppService {

  menu$: Observable<MenuItem[]>;

  constructor(
    private overLayService: OverlayService,
    private menuStateService: MenuStateService,
  ) {
    this.menu$ = this.menuStateService.menu$;
  }

  get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overLayService.enable(),
      hideMask: () => this.overLayService.disable(),
      navigate: (location: NavLocation) => this.setActiveItemAndNavigate(location),
      updateNavLocation: (location: NavLocation) => this.setActiveItem(location),
    };
  }

  private setActiveItemAndNavigate(location: NavLocation): void {
    return this.activeMenuItem$(location)
    .subscribe(item => {
      if (item) {
        this.menuStateService.setActiveItemAndNavigate(item);
      } else {
        console.error(this.message(location));
      }
    }).unsubscribe();
  }

  private setActiveItem(location: NavLocation): void {
    return this.activeMenuItem$(location)
    .subscribe(item => {
      if (item) {
        this.menuStateService.setActiveItem(item);
      } else {
        console.error(this.message(location));
      }
    }).unsubscribe();
  }

  private activeMenuItem$: (location: NavLocation) => Observable<any> =
    (location: NavLocation) => this.menu$.pipe(
      mergeAll(),
      first(item => (item as MenuItemLink).appPath === location.path, ''),
    )

  private message(location): string {
    return `Cannot find associated menu item for Navlocation:{${JSON.stringify(location)}}`;
  }
}
