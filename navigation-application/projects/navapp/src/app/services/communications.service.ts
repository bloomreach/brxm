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

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { Connection } from '../models/connection.model';
import { FailedConnection } from '../models/failed-connection.model';
import { DeepLinkingService } from '../routing/deep-linking.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  constructor(
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private deepLinkingService: DeepLinkingService,
    private overlay: OverlayService,
    private busyIndicatorService: BusyIndicatorService,
    private settings: GlobalSettingsService,
  ) {}

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

        this.deepLinkingService.navigateByAppUrl(app.url, location.path, location.breadcrumbLabel);
      },
      updateNavLocation: (location: NavLocation) => {
        const navItem = this.navConfigService.findNavItem(this.clientAppService.activeApp.url, location.path);
        if (!navItem) {
          console.error(`updateNavLocation was called with not allowed location url: '${location.path}'`);
          return;
        }

        this.deepLinkingService.updateByAppUrl(
          navItem.appIframeUrl,
          location.path,
          location.breadcrumbLabel,
        );
      },
    };
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
