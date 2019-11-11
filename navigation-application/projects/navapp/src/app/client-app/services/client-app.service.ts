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

import { Location } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { ChildConfig, NavItem } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { BehaviorSubject, Observable, ReplaySubject, Subject } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { bufferTime, first, map, switchMap, tap } from 'rxjs/operators';

import { CriticalError } from '../../error-handling/models/critical-error';
import { Connection } from '../../models/connection.model';
import { AppSettings } from '../../models/dto/app-settings.dto';
import { FailedConnection } from '../../models/failed-connection.model';
import { APP_SETTINGS } from '../../services/app-settings';
import { NavItemService } from '../../services/nav-item.service';
import { ClientApp } from '../models/client-app.model';

interface ClientAppWithConfig {
  app: ClientApp;
  config: ChildConfig;
}

@Injectable()
export class ClientAppService {
  private uniqueURLs = new BehaviorSubject<string[]>([]);
  private connectedApps: Map<string, ClientAppWithConfig> = new Map<string, ClientAppWithConfig>();
  private activeAppId = new BehaviorSubject<string>(undefined);
  private connectionCounter$ = new ReplaySubject<Connection>();
  private userActivityReceived$ = new Subject<ClientApp>();

  constructor(
    @Inject(APP_SETTINGS) private appSettings: AppSettings,
    private navItemService: NavItemService,
    private logger: NGXLogger,
  ) { }

  get urls$(): Observable<string[]> {
    return this.uniqueURLs.asObservable();
  }

  get apps(): ClientApp[] {
    return Array.from(this.connectedApps.values()).map(c => c.app);
  }

  get activeApp(): ClientApp {
    const activeAppId = this.activeAppId.value;

    if (!activeAppId) {
      return undefined;
    }

    return this.getApp(activeAppId);
  }

  get doesActiveAppSupportSites(): boolean {
    const activeApp = this.activeApp;

    if (!activeApp) {
      return false;
    }

    return this.doesAppSupportSites(this.activeApp);
  }

  init(): Promise<void> {
    const navItems = this.navItemService.navItems;
    const uniqueURLs = this.filterUniqueURLs(navItems);
    this.uniqueURLs.next(uniqueURLs);

    this.logger.debug('Client app iframes are expected to be loaded', uniqueURLs);

    return this.uniqueURLs.pipe(
      switchMap(urls => this.waitForConnections(urls.length)),
      map(connections => this.discardFailedConnections(connections)),
      map(connections => connections.map(c => this.createClientApp(c))),
      switchMap(apps => fromPromise(this.fetchAppConfigs(apps))),
      tap(appsWithConfigs => appsWithConfigs.forEach(awc => this.connectedApps.set(awc.app.url, awc))),
      first(),
    ).toPromise() as Promise<any>;
  }

  activateApplication(appId: string): void {
    if (!this.connectedApps.has(appId)) {
      throw new Error(`An attempt to active unknown app '${appId}'`);
    }

    this.activeAppId.next(appId);
  }

  addConnection(connection: Connection): void {
    const uniqueURLs = this.uniqueURLs.value;
    const connectionUrl = Location.stripTrailingSlash(connection.appUrl);

    const url = uniqueURLs.find(x => Location.stripTrailingSlash(x) === connectionUrl);

    if (!url) {
      const message = `An attempt to register the connection to an unknown url '${connection.appUrl}'`;

      this.logger.error(message);
      this.connectionCounter$.next(new FailedConnection(connection.appUrl, message));

      return;
    }

    // Fix extra/missing trailing slash issue
    connection.appUrl = url;

    this.logger.debug(`Connection is established to the iframe '${url}'`);

    this.connectionCounter$.next(connection);
  }

  getApp(appUrl: string): ClientApp {
    const app = this.connectedApps.has(appUrl);
    if (!app) {
      throw new Error(`Unable to find the app with id = ${appUrl}`);
    }

    return this.connectedApps.get(appUrl).app;
  }

  getAppConfig(appUrl: string): ChildConfig {
    const app = this.connectedApps.has(appUrl);
    if (!app) {
      throw new Error(`Unable to find the app with id = ${appUrl}`);
    }

    return this.connectedApps.get(appUrl).config;
  }

  onUserActivity(): Promise<void> {
    this.userActivityReceived$.next(this.activeApp);
    return Promise.resolve();
  }

  private filterUniqueURLs(navItems: NavItem[]): string[] {
    const uniqueUrlsSet = navItems.reduce((uniqueUrls, config) => {
      uniqueUrls.add(config.appIframeUrl);
      return uniqueUrls;
    }, new Set<string>());

    return Array.from(uniqueUrlsSet.values());
  }

  private doesAppSupportSites(app: ClientApp): boolean {
    return this.getAppConfig(app.url).showSiteDropdown || false;
  }

  private waitForConnections(expectedNumber: number): Observable<Connection[]> {
    return this.connectionCounter$.pipe(
      bufferTime(this.appSettings.iframesConnectionTimeout * 1.5, undefined, expectedNumber),
    );
  }

  private fetchAppConfigs(apps: ClientApp[]): Promise<ClientAppWithConfig[]> {
    const configPromises: Promise<ClientAppWithConfig>[] = [];

    const fetchConfig = (app: ClientApp) => this.fetchAppConfig(app).then(config => ({ app, config }));

    apps.forEach(app => configPromises.push(fetchConfig(app)));

    return Promise.all(configPromises);
  }

  private fetchAppConfig(app: ClientApp): Promise<ChildConfig> {
    return app.api.getConfig ?
      app.api.getConfig().then(config => {
        if (!config) {
          this.logger.warn(`The app '${app.url}' returned an empty config`);
          config = { apiVersion: 'unknown' };
        }

        if (!config.apiVersion) {
          this.logger.warn(`The app '${app.url}' returned a config with an empty version`);
          config.apiVersion = 'unknown';
        }

        this.logger.info(`Connected API '${app.url}' version ${config.apiVersion}`);

        return config;
      }) :
      Promise.resolve({ apiVersion: 'unknown' } as ChildConfig);
  }

  private discardFailedConnections(connections: Connection[]): Connection[] {
    const successfulConnections = connections.filter(c => !(c instanceof FailedConnection));

    if (successfulConnections.length === 0) {
      throw new CriticalError(
        'Unable to connect to the client applications',
        'All connections to the client applications are failed',
      );
    }

    return successfulConnections;
  }

  private createClientApp(connection: Connection): ClientApp {
    return new ClientApp(connection.appUrl, connection.api);
  }
}
