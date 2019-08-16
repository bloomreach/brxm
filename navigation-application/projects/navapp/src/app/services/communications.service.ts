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
import { connectToChild, NavLocation, ParentApi, SiteId } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuItemLink } from '../main-menu/models/menu-item-link.model';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { Connection } from '../models/connection.model';
import { FailedConnection } from '../models/failed-connection.model';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  private activeMenuItem: MenuItemLink;
  private unsubscribe = new Subject();

  constructor(
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private breadcrumbsService: BreadcrumbsService,
    private overlay: OverlayService,
    private busyIndicatorService: BusyIndicatorService,
    private settings: GlobalSettingsService,
  ) {
    menuStateService.activeMenuItem$.pipe(
      takeUntil(this.unsubscribe),
      tap(x => this.activeMenuItem = x),
    ).subscribe(activeMenuItem => {
      this.breadcrumbsService.clearSuffix();
      this.navigate(activeMenuItem.appUrl, activeMenuItem.appPath);
    });
  }

  get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overlay.enable(),
      hideMask: () => this.overlay.disable(),
      navigate: (location: NavLocation) => {
        // We need to use caller's appUrl but instead (for first implementation) we just look for the first
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

        this.menuStateService.activateMenuItem(app.url, location.path);
        this.breadcrumbsService.setSuffix(location.breadcrumbLabel);

        // Current app can be different in that moment it's better to use caller app's id
        if (app.url !== this.clientAppService.activeApp.url) {
          this.navigate(app.url, location.path);
        }
      },
      updateNavLocation: (location: NavLocation) => {
        this.breadcrumbsService.setSuffix(location.breadcrumbLabel);
        this.menuStateService.activateMenuItem(
            this.clientAppService.activeApp.url,
            location.path,
        );
      },
    };
  }

  navigate(appId: string, path: string, flags?: { [key: string]: string | number | boolean }): Promise<void> {
    this.busyIndicatorService.show();
    const app = this.clientAppService.getApp(appId);

    if (!app) {
      throw new Error(`There is no app with id="${appId}"`);
    }

    if (!app.api) {
      throw new Error(`The app with id="${appId}" is not connected to the nav app`);
    }

    return app.api.navigate({ path }, flags).then(() => {
      this.clientAppService.activateApplication(appId);
      this.busyIndicatorService.hide();
    });
  }

  navigateToDefaultPage(): Promise<void> {
    if (!this.activeMenuItem) {
      return Promise.reject('There is no the selected menu item.');
    }

    this.breadcrumbsService.clearSuffix();

    return this.navigate(this.activeMenuItem.appUrl, this.activeMenuItem.appPath, {forceRefresh: true});
  }

  updateSelectedSite(siteId: SiteId): Promise<void> {
    this.busyIndicatorService.show();

    return this.clientAppService.activeApp.api.updateSelectedSite(siteId).then(() => {
      const updatePromises = this.clientAppService.apps
        .filter(app => app.api && app.api.updateSelectedSite && app !== this.clientAppService.activeApp)
        .map(app => app.api.updateSelectedSite());

      return Promise.all(updatePromises).then(() => this.busyIndicatorService.hide());
    });
  }

  logout(): Promise<void> {
    this.busyIndicatorService.show();

    return this.clientAppService.logoutApps().then(
      () => this.navConfigService.logout(),
    ).then(() => this.busyIndicatorService.hide());
  }

  connectToChild(iframe: HTMLIFrameElement): Promise<void> {
    const appUrl = iframe.src;

    return connectToChild({
      iframe,
      methods: this.parentApiMethods,
      timeout: this.settings.appSettings.iframesConnectionTimeout,
    }).then(
      api => this.clientAppService.addConnection(new Connection(appUrl, api)),
      error => this.clientAppService.addConnection(new FailedConnection(appUrl, error)),
    );
  }

  private findApp(path: string): ClientApp {
    return this.clientAppService.apps.find(
      app => !!this.navConfigService.findNavItem(app.url, path),
    );
  }
}
