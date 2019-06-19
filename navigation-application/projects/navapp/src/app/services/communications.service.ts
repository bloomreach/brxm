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

import { Injectable, OnDestroy } from '@angular/core';
import { NavLocation, ParentApi } from '@bloomreach/navapp-communication';
import { Observable, Subject } from 'rxjs';
import { map, mergeMap, takeUntil } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services';
import { MenuStateService } from '../main-menu/services';

import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

@Injectable({
  providedIn: 'root',
})
export class CommunicationsService implements OnDestroy {
  private appIds: string[] = [];
  private activeAppId: string;
  private unsubscribe = new Subject();

  private static async resolveAlways<T>(p: Promise<T>): Promise<any> {
    try {
      return await p;
    } catch (err) {
      return err;
    }
  }

  constructor(
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private overlay: OverlayService,
    private navConfigService: NavConfigService,
  ) {
    clientAppService.apps$.pipe(
      // url === id for client applications
      map(apps => apps.map(x => x.url)),
      takeUntil(this.unsubscribe),
    ).subscribe(appIds => this.appIds = appIds);

    clientAppService.activeAppId$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(appId => this.activeAppId = appId);

    menuStateService.activeMenuItem$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(activeMenuItem => this.navigate(activeMenuItem.appId, activeMenuItem.appPath));
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
          console.error(`Cannot find associated menu item for Navlocation:{${JSON.stringify(location)}}`);
        }

        this.menuStateService.activateMenuItem(appId, location.path);
        this.navigate(appId, location.path);
      },
      updateNavLocation: (location: NavLocation) => this.menuStateService.activateMenuItem(this.activeAppId, location.path),
    };
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  navigate(clientAppId: string, path: string): Promise<void> {
    return this.clientAppService
      .getApp(clientAppId)
      .api.navigate({ path })
      .then(() => {
        this.clientAppService.activateApplication(clientAppId);
      });
  }

  logout(): Observable<any[]> {
    return this.clientAppService.apps$.pipe(
      mergeMap(apps => Promise.all(this.logoutApps(apps))),
    );
  }

  private logoutApps(clientApps: ClientApp[]): Promise<any>[] {
    return clientApps
      .filter(app => app.api.logout)
      .map(app => CommunicationsService.resolveAlways(app.api.logout()));
  }

  private findAppId(path: string): string {
    return this.appIds.find(appId => !!this.navConfigService.findNavItem(appId, path));
  }
}
