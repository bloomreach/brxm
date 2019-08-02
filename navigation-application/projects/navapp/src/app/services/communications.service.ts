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
import { NavLocation, ParentApi, SiteId } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
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
  private apps: ClientApp[] = [];
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
      takeUntil(this.unsubscribe),
    ).subscribe(apps => (this.apps = apps));

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
        const app = this.findApp(location.path);

        if (!app) {
          console.error(
            `Cannot find associated menu item for Navlocation:{${JSON.stringify(
              location,
            )}}`,
          );
          return;
        }

        this.menuStateService.activateMenuItem(app.id, location.path);
        this.breadcrumbsService.setSuffix(location.breadcrumbLabel);

        // Current app can be different in that moment it's better to use caller app's id
        if (app.id !== this.clientAppService.activeApp.id) {
          this.navigate(app.id, location.path);
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

  navigate(appId: string, path: string): Promise<void> {
    const app = this.clientAppService.getApp(appId);

    if (!app) {
      throw new Error(`There is no app with id="${appId}"`);
    }

    if (!app.api) {
      throw new Error(`The app with id="${appId}" is not connected to the nav app`);
    }

    return app.api.navigate({ path }).then(() => {
      this.clientAppService.activateApplication(appId);
    });
  }

  navigateToDefaultPage(): Promise<void> {
    if (!this.activeMenuItem) {
      return Promise.reject('There is no the selected menu item.');
    }

    let path = this.activeMenuItem.appPath;

    if (path === 'channelmanager') {
      path += '/';
    }

    this.breadcrumbsService.clearSuffix();

    return this.navigate(this.activeMenuItem.appId, path);
  }

  updateSelectedSite(siteId: SiteId): Promise<void[]> {
    return this.clientAppService.activeApp.api.updateSelectedSite(siteId).then(() => {
      const updatePromises = this.apps
        .filter(app => app.api.updateSelectedSite && app !== this.clientAppService.activeApp)
        .map(app => app.api.updateSelectedSite());

      return Promise.all(updatePromises);
    });
  }

  logout(): Promise<void[]> {
    return this.clientAppService.logoutApps().then(
      () => this.navConfigService.logout(),
    );
  }

  private findApp(path: string): ClientApp {
    return this.apps.find(
      app => !!this.navConfigService.findNavItem(app.id, path),
    );
  }
}
