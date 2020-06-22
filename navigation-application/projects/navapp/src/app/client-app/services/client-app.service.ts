/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { bufferTime, filter, first, map, mergeMap, publishReplay, refCount, takeUntil, tap } from 'rxjs/operators';

import { CriticalError } from '../../error-handling/models/critical-error';
import { Connection } from '../../models/connection.model';
import { AppSettings } from '../../models/dto/app-settings.dto';
import { FailedConnection } from '../../models/failed-connection.model';
import { APP_SETTINGS } from '../../services/app-settings';
import { ClientApp } from '../models/client-app.model';

interface ClientAppWithConfig {
  app: ClientApp;
  config: ChildConfig;
}

@Injectable()
export class ClientAppService {
  private readonly uniqueURLs$ = new BehaviorSubject<string[]>([]);
  private readonly connection$ = new Subject<Connection>();
  private readonly connectedAppWithConfig$ = new Subject<ClientAppWithConfig>();
  private readonly reset$ = new Subject<void>();

  private readonly connectedApps: Map<string, ClientAppWithConfig> = new Map<string, ClientAppWithConfig>();
  private activeAppUrl: string;
  private allAppsAreConnectedOrTimeout = false;

  constructor(
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
    private readonly logger: NGXLogger,
  ) {
    this.transformConnectionsToApps(this.connection$).subscribe(this.connectedAppWithConfig$);
  }

  get urls$(): Observable<string[]> {
    return this.uniqueURLs$.asObservable();
  }

  get appConnected$(): Observable<ClientApp> {
    return this.connectedAppWithConfig$.pipe(
      map(appWithConfig => appWithConfig.app),
    );
  }

  get apps(): ClientApp[] {
    return Array.from(this.connectedApps.values()).map(c => c.app);
  }

  get activeApp(): ClientApp {
    if (!this.activeAppUrl) {
      return undefined;
    }

    try {
      return this.getApp(this.activeAppUrl);
    } catch {
      return undefined;
    }
  }

  get doesActiveAppSupportSites(): boolean {
    const activeApp = this.activeApp;

    if (!activeApp) {
      return false;
    }

    return this.doesAppSupportSites(this.activeApp);
  }

  async init(navItems: NavItem[]): Promise<void> {
    this.reset$.next();
    // it forces the microtask caused by this.reset$.next() to be executed which lead to finishing previous async init() invocation
    // in other words it resets the state to perform another initialization
    await (new Promise(r => setTimeout(r, 0)));

    this.allAppsAreConnectedOrTimeout = false;

    const uniqueURLs = this.filterUniqueURLs(navItems);

    this.logger.debug(`Client app iframes are expected to be loaded (${uniqueURLs.length})`, uniqueURLs);

    const allAppsAreConnectedOrTimeoutPromise = this.waitForAllAppsToBeConnectedOrTimeout(
      this.connectedAppWithConfig$,
      uniqueURLs.length,
      this.appSettings.iframesConnectionTimeout * 1.5,
    ).pipe(takeUntil(this.reset$)).toPromise();

    this.reuseAlreadyConnectedAppsWithoutSitesSupport(uniqueURLs);

    // Propagate urls of apps to load
    this.uniqueURLs$.next(uniqueURLs);

    await allAppsAreConnectedOrTimeoutPromise;

    this.allAppsAreConnectedOrTimeout = true;
  }

  activateApplication(appUrl: string): void {
    if (!this.connectedApps.has(appUrl)) {
      throw new Error(`An attempt to active unknown app '${appUrl}'`);
    }

    this.activeAppUrl = appUrl;
  }

  addConnection(connection: Connection): void {
    if (this.allAppsAreConnectedOrTimeout) {
      throw new Error('An attempt to register a connection after all expected connections are registered or timeout has expired');
    }

    const uniqueURLs = this.uniqueURLs$.value;
    const connectionUrl = Location.stripTrailingSlash(connection.appUrl);

    const url = uniqueURLs.find(x => Location.stripTrailingSlash(x) === connectionUrl);

    if (!url) {
      const message = `An attempt to register a connection to an unknown url '${connection.appUrl}'`;

      this.logger.error(message);

      return;
    }

    if (connection instanceof FailedConnection) {
      this.logger.warn(`Failed to establish a connection to the iframe '${url}'`, connection.reason);
    } else {
      this.logger.debug(`Connection is established to the iframe '${url}'`);
    }

    // Fix extra/missing trailing slash issue
    connection.appUrl = url;

    this.connection$.next(connection);
  }

  getApp(appUrl: string): ClientApp {
    const app = this.connectedApps.has(appUrl);
    if (!app) {
      throw new Error(`Unable to find the app '${appUrl}'`);
    }

    return this.connectedApps.get(appUrl).app;
  }

  getAppConfig(appUrl: string): ChildConfig {
    const app = this.connectedApps.has(appUrl);
    if (!app) {
      throw new Error(`Unable to find the app '${appUrl}'`);
    }

    return this.connectedApps.get(appUrl).config;
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

  private transformConnectionsToApps(connection$: Observable<Connection>): Observable<ClientAppWithConfig> {
    return connection$.pipe(
      filter(connection => !(connection instanceof FailedConnection)),
      mergeMap(connection => this.createClientAppWithConfig(connection)),
      tap(appWithConfig => this.connectedApps.set(appWithConfig.app.url, appWithConfig)),
      publishReplay(),
      refCount(),
    );
  }

  private async createClientAppWithConfig(connection: Connection): Promise<ClientAppWithConfig> {
    const app = new ClientApp(connection.appUrl, connection.api);

    const config = await this.fetchAppConfig(app);

    return {
      app,
      config,
    };
  }

  private async fetchAppConfig(app: ClientApp): Promise<ChildConfig> {
    this.logger.debug(`Fetching app config for '${app.url}'`);

    const fallbackConfig = { apiVersion: 'unknown' };

    if (!app.api.getConfig) {
      this.logger.warn(`getConfig() method is not defined in api for the app '${app.url}'`);

      return fallbackConfig;
    }

    try {
      let config = await app.api.getConfig();

      this.logger.debug(`App config is fetched for '${app.url}'`, config);

      if (!config) {
        this.logger.warn(`The app '${app.url}' returned an empty config`);
        config = fallbackConfig;
      }

      if (!config.apiVersion) {
        this.logger.warn(`The app '${app.url}' returned a config with an empty version`);
        config.apiVersion = fallbackConfig.apiVersion;
      }

      this.logger.info(`Connected API '${app.url}' version ${config.apiVersion}`);

      return config;
    } catch (e) {
      this.logger.warn(`Unable to load config for '${app.url}'. Reason: '${e}'.`);

      return { apiVersion: 'unknown' };
    }
  }

  private waitForAllAppsToBeConnectedOrTimeout(
    connectedApp$: Observable<ClientAppWithConfig>,
    expectedNumber: number,
    timeout: number,
  ): Observable<ClientAppWithConfig[]> {
    return connectedApp$.pipe(
      bufferTime(timeout, undefined, expectedNumber),
      first(),
      tap(apps => {
        if (apps.length > 0) {
          return;
        }

        throw new CriticalError(
          'ERROR_UNABLE_TO_CONNECT_TO_CLIENT_APP',
          'All connections to the client applications have failed failed',
        );
      }),
    );
  }

  private reuseAlreadyConnectedAppsWithoutSitesSupport(newAppUrls: string[]): void {
    for (const [url, appWithConfig] of this.connectedApps) {
      if (newAppUrls.includes(url) && !appWithConfig.config.showSiteDropdown) {
        continue;
      }

      this.connectedApps.delete(url);
    }

    for (const [url, appWithConfig] of this.connectedApps) {
      this.logger.debug(`Reuse existing connection for the app without sites support '${url}'`);
      this.connectedAppWithConfig$.next(appWithConfig);
    }
  }
}
