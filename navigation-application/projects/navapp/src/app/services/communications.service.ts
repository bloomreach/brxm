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

import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';

import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService {
  private appIds: string[] = [];

  constructor(
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private overlayService: OverlayService,
    private navConfigService: NavConfigService,
  ) {
    clientAppService.apps$.subscribe(apps => {
      this.appIds = apps.map(app => app.id);
    });

    menuStateService.activeMenuItem$.subscribe(activeMenuItem =>
      this.navigate(activeMenuItem.appId, activeMenuItem.appPath),
    );
  }

  get parentApiMethods(): ParentApi {
    return {
      showMask: () => this.overlayService.enable(),
      hideMask: () => this.overlayService.disable(),
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
        this.navigate(appId, location.path);
      },
      updateNavLocation: (location: NavLocation) =>
        this.menuStateService.activateMenuItem(
          this.clientAppService.activeApp.id,
          location.path,
        ),
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
