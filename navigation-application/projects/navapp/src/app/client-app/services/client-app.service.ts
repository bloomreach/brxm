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
import { BehaviorSubject, Observable, ReplaySubject } from 'rxjs';
import { bufferCount, filter, first, map, switchMap, tap } from 'rxjs/operators';

import { Connection } from '../../models/connection.model';
import { NavItem } from '../../models/dto/nav-item.dto';
import { FailedConnection } from '../../models/failed-connection.model';
import { NavConfigService } from '../../services/nav-config.service';
import { ClientApp } from '../models/client-app.model';

@Injectable()
export class ClientAppService {
  private uniqueURLs = new BehaviorSubject<string[]>([]);
  private connectedApps: ClientApp[] = [];
  private activeAppId = new BehaviorSubject<string>(undefined);
  private connection$ = new ReplaySubject<Connection>();

  constructor(private navConfigService: NavConfigService) {}

  get urls$(): Observable<string[]> {
    return this.uniqueURLs.asObservable();
  }

  get apps(): ClientApp[] {
    return this.connectedApps;
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

  init(): Promise<ClientApp[]> {
    this.navConfigService.navItems$.pipe(
      filter(x => x && x.length > 0),
      map(navItems => this.filterUniqueURLs(navItems)),
    ).subscribe(x => this.uniqueURLs.next(x));

    return this.uniqueURLs.pipe(
      switchMap(uniqueURLs => this.waitForConnections(uniqueURLs.length)),
      map(connections => this.discardFailedConnections(connections)),
      map(connections => connections.map(c => this.createClientApp(c))),
      tap(apps => this.connectedApps = apps),
      first(),
    ).toPromise();
  }

  activateApplication(appId: string): void {
    this.activeAppId.next(appId);
  }

  addConnection(connection: Connection): void {
    const uniqueURLs = this.uniqueURLs.value;

    if (!uniqueURLs.includes(connection.appUrl) && !uniqueURLs.includes(`${connection.appUrl}/`)) {
      console.error(`An attempt to register the connection to unknown url = ${connection.appUrl}`);
      return;
    }

    this.connection$.next(connection);
  }

  getApp(appUrl: string): ClientApp {
    const app = this.connectedApps.find(x => x.url === appUrl);
    if (!app) {
      throw new Error(`Unable to find the app with id = ${appUrl}`);
    }

    return app;
  }

  logoutApps(): Promise<void[]> {
    return Promise.all(this.connectedApps.map(
      app => app.api.logout(),
    ));
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

  private waitForConnections(expectedNumber: number): Observable<Connection[]> {
    return this.connection$.pipe(
      bufferCount(expectedNumber),
    );
  }

  private discardFailedConnections(connections: Connection[]): Connection[] {
    return connections.filter(c => !(c instanceof FailedConnection));
  }

  private createClientApp(connection: Connection): ClientApp {
    return new ClientApp(connection.appUrl, connection.api);
  }
}
