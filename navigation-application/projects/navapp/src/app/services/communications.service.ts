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
import { Subject } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuItemLink } from '../main-menu/models/menu-item-link.model';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  private appIds: string[] = [];
  private activeMenuItem: MenuItemLink;
  private unsubscribe = new Subject();

  constructor(
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private breadcrumbsService: BreadcrumbsService,
    private overlay: OverlayService,
  ) {
    clientAppService.apps$.pipe(
      // url === id for client applications
      map(apps => apps.map(x => x.url)),
      takeUntil(this.unsubscribe),
    ).subscribe(appIds => (this.appIds = appIds));

    menuStateService.activeMenuItem$.pipe(
      takeUntil(this.unsubscribe),
      tap(x => this.activeMenuItem = x),
    ).subscribe(activeMenuItem => {
      this.breadcrumbsService.clearSuffix();
      this.navigate(activeMenuItem.appId, activeMenuItem.appPath);
    });
  }

  get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overlay.enable(),
      hideMask: () => this.overlay.disable(),
      navigate: (location: NavLocation) => {
        // We need to use caller's appId but instead (for first implementation) we just look for the first
        // app's id which contains the specified path
        const appId = this.findAppId(location.path);

        if (!appId) {
          console.error(
            `Cannot find associated menu item for Navlocation:{${JSON.stringify(
              location,
            )}}`,
          );
        }

        this.menuStateService.activateMenuItem(appId, location.path);
        this.breadcrumbsService.setSuffix(location.breadcrumbLabel);

        // Current app can be different in that moment it's better to use caller app's id
        if (appId !== this.clientAppService.activeApp.id) {
          this.navigate(appId, location.path);
        }
      },
      updateNavLocation: (location: NavLocation) => {
        this.menuStateService.activateMenuItem(
          this.clientAppService.activeApp.id,
          location.path,
        );
        this.breadcrumbsService.setSuffix(location.breadcrumbLabel);
      },
    };
  }

  navigate(clientAppId: string, path: string): Promise<void> {
    return this.clientAppService
      .getApp(clientAppId)
      .api.navigate({ path })
      .then(() => {
        this.clientAppService.activateApplication(clientAppId);
      });
  }

  navigateToDefaultPage(): Promise<void> {
    if (!this.activeMenuItem) {
      return Promise.reject('There is no the selected menu item.');
    }

    this.breadcrumbsService.clearSuffix();

    return this.navigate(this.activeMenuItem.appId, this.activeMenuItem.appPath);
  }

  updateSite(siteId: number): Promise<void[]> {
    return this.clientAppService.activeApp.api.updateSite(siteId).then(() => {
      const activeApp = this.clientAppService.activeApp;

      const updatePromises = this.clientAppService.appsWithSitesSupport.map(
        app => {
          if (app === activeApp) {
            return;
          }

          return app.api.updateSite();
        },
      );

      return Promise.all(updatePromises);
    });
  }

  logout(): void {
    this.clientAppService.apps$.subscribe(apps => {
      apps.filter(app => app.api.logout).forEach(app => app.api.logout());
    });
  }

  private findAppId(path: string): string {
    return this.appIds.find(
      appId => !!this.navConfigService.findNavItem(appId, path),
    );
  }
}
