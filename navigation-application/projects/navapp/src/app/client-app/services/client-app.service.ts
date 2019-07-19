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
import { ChildPromisedApi } from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable, ReplaySubject } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavItem } from '../../models/dto/nav-item.dto';
import { NavConfigService } from '../../services/nav-config.service';
import { ClientApp } from '../models/client-app.model';

@Injectable()
export class ClientAppService {
  private apps = new BehaviorSubject<ClientApp[]>([]);
  private activeAppId = new BehaviorSubject<string>(undefined);
  private connectionEstablished = new ReplaySubject<ClientApp>(1);
  private allConnectionsEstablished = new ReplaySubject<void>(1);

  constructor(private navConfigService: NavConfigService) {}

  get apps$(): Observable<ClientApp[]> {
    return this.apps.asObservable();
  }

  get connectionEstablished$(): Observable<ClientApp> {
    return this.connectionEstablished.asObservable();
  }

  get allConnectionsEstablished$(): Observable<void> {
    return this.allConnectionsEstablished.asObservable();
  }

  get activeApp(): ClientApp {
    const activeAppId = this.activeAppId.value;

    if (!activeAppId) {
      return undefined;
    }

    try {
      return this.getApp(activeAppId);
    } catch {
      throw new Error(`Unable to find the active app with id = ${activeAppId}`);
    }
  }

  get doesActiveAppSupportSites(): boolean {
    return this.doesAppSupportSites(this.activeApp);
  }

  init(): void {
    this.navConfigService.navItems$
      .pipe(
        map(navItems => this.filterUniqueURLs(navItems)),
        map(uniqueURLs => uniqueURLs.map(url => new ClientApp(url))),
      )
      .subscribe(apps => this.apps.next(apps));
  }

  activateApplication(appId: string): void {
    this.activeAppId.next(appId);
  }

  addConnection(appId: string, api: ChildPromisedApi): void {
    this.updateApp(appId, api);

    this.connectionEstablished.next(this.getApp(appId));

    this.checkEstablishedConnections();
  }

  getApp(appId: string): ClientApp {
    const apps = this.apps.value;
    const app = apps.find(a => a.id === appId);
    if (!app) {
      throw new Error(`There is no connection to an iframe with id = ${appId}`);
    }

    return app;
  }

  logoutApps(): Promise<void[]> {
    const apps = this.apps.value;
    return Promise.all(apps.map(
      app => app.api.logout(),
    ));
  }

  private updateApp(appId: string, api: ChildPromisedApi): void {
    const apps = this.apps.value;
    const appToUpdateIndex = apps.findIndex(app => app.id === appId);

    if (appToUpdateIndex === -1) {
      return;
    }

    const updatedApp = new ClientApp(apps[appToUpdateIndex].url);
    updatedApp.api = api;

    apps[appToUpdateIndex] = updatedApp;

    this.apps.next(apps);
  }

  private checkEstablishedConnections(): void {
    const apps = this.apps.value;
    const allConnected = apps.every(a => a.api !== undefined);

    if (allConnected) {
      this.allConnectionsEstablished.next();
      this.allConnectionsEstablished.complete();
    }
  }

  private filterUniqueURLs(navItems: NavItem[]): string[] {
    const uniqueUrlsSet = navItems.reduce((uniqueUrls, config) => {
      uniqueUrls.add(config.appIframeUrl);
      return uniqueUrls;
    }, new Set<string>());

    return Array.from(uniqueUrlsSet.values());
  }

  private doesAppSupportSites(app: ClientApp): boolean {
    return !!(app && app.api && app.api.updateSelectedSite);
  }
}
