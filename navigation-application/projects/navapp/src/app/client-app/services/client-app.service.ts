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
import { BehaviorSubject, merge, Observable, of, race, Subject, throwError } from 'rxjs';
import { bufferTime, filter, first, map, mapTo, mergeMap, publishReplay, refCount, switchMap, take, takeUntil, tap } from 'rxjs/operators';

import { CriticalError } from '../../error-handling/models/critical-error';
import { TimeoutError } from '../../error-handling/models/timeout-error';
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
  private uniqueURLs: string[] = [];
  private readonly clientAppUrls$ = new BehaviorSubject<string[]>([]);
  private readonly connectionError$ = new Subject<{ url: string, reason?: string }>();
  private readonly connectionHandled$ = new Subject<string>();
  private readonly connectedApps: Map<string, ClientAppWithConfig> = new Map<string, ClientAppWithConfig>();
  private activeAppUrl: string;

  constructor(
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
    private readonly logger: NGXLogger,
  ) {
  }

  get urls$(): Observable<string[]> {
    return this.clientAppUrls$.asObservable();
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
    this.uniqueURLs = this.filterUniqueURLs(navItems);
    this.clientAppUrls$.next([]);
    this.connectedApps.clear();

    this.logger.debug(`Potential ClientApps to connect:`, this.uniqueURLs);
  }

  activateApplication(appUrl: string): void {
    if (!this.connectedApps.has(appUrl)) {
      throw new Error(`An attempt to activate unknown app '${appUrl}'`);
    }

    this.activeAppUrl = appUrl;
  }

  async initiateClientApp(appUrl: string): Promise<unknown> {
    this.logger.debug(`Initiating ClientApp ${appUrl}`);

    try {
      const app = this.getApp(appUrl);
      if (app) {
        this.logger.debug(`ClientApp ${appUrl} is already present and connected`);
        return;
      }
    } catch (error) {
      this.logger.debug(`ClientApp ${appUrl} was not yet initiated`);
    }

    this.activeAppUrl = undefined;

    const currentUrls = this.clientAppUrls$.value;

    if (!currentUrls.includes(appUrl)) {
      this.logger.debug(`ClientApp ${appUrl} connecting...`);
      this.clientAppUrls$.next([...currentUrls, appUrl]);
    }

    this.logger.debug(`ClientApp ${appUrl} has been initiated and is waiting for connection`);

    return race(
      this.connectionHandled$,
      this.connectionError$.pipe(
        switchMap(error => throwError(new TimeoutError('ERROR_TIMEOUT_DESCRIPTION', `ClientApp ${error.url} failed to connect: ${error.reason}`))),
      ),
    ).pipe(
      filter(url => url === appUrl),
      take(1),
    ).toPromise();
  }

  async addConnection(connection: Connection): Promise<void> {
    const connectionUrl = Location.stripTrailingSlash(connection.appUrl);
    const url = this.uniqueURLs.find(x => Location.stripTrailingSlash(x) === connectionUrl);

    if (!url) {
      const message = `An attempt to register a connection to an unknown url '${connection.appUrl}'`;
      this.logger.error(message);
      return;
    }

    this.logger.debug(`Connection is established to the iframe '${url}'`);
    connection.appUrl = url;
    const appWithConfig = await this.createClientAppWithConfig(connection);
    this.connectedApps.set(appWithConfig.app.url, appWithConfig);
    this.connectionHandled$.next(url);
  }

  handleFailedConnection({ appUrl, reason }: FailedConnection): void {
    appUrl = Location.stripTrailingSlash(appUrl);

    this.logger.error(`Failed to establish a connection to the iframe '${appUrl}'`, reason);

    const currentUrls = this.clientAppUrls$.value;
    const index = currentUrls.findIndex(url => url === appUrl);
    currentUrls.splice(index, 1);
    this.clientAppUrls$.next(currentUrls);

    this.connectionError$.next({
      url: appUrl,
      reason,
    });
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
}
