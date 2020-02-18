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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import {
  ChildApi,
  NavItem,
  Site,
  SiteId,
} from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { tap } from 'rxjs/operators';

import { AppSettings } from '../models/dto/app-settings.dto';
import { ConfigResource } from '../models/dto/config-resource.dto';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';

export interface Configuration {
  navItems: NavItem[];
  sites: Site[];
  selectedSiteId: SiteId;
}

type ChildApiMethod<T extends string> = Extract<keyof(ChildApi), T>;

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  private navItemsFromRestCache: NavItem[] = [];

  constructor(
    private readonly http: HttpClient,
    private readonly location: Location,
    private readonly connectionService: ConnectionService,
    private readonly logger: NGXLogger,
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
  ) { }

  async fetchNavigationConfiguration(): Promise<Configuration> {
    const resources = this.appSettings.navConfigResources;

    this.navItemsFromRestCache = [];

    try {
      const navItemsPerResourcePromises = resources.map(async r => {
        const navItems = await this.fetchNavItems(r);

        if (r.resourceType === 'REST' || r.resourceType === 'INTERNAL_REST') {
          this.navItemsFromRestCache = this.navItemsFromRestCache.concat(navItems);
        }

        return navItems;
      });

      const [navItemsPerResource, sitesPerResource, selectedSitePerResource] = await Promise.all([
        Promise.all(navItemsPerResourcePromises),
        Promise.all(resources.map(r => this.fetchSites(r))),
        Promise.all(resources.map(r => this.fetchSelectedSite(r))),
      ]);

      const selectedSite = selectedSitePerResource.find(x => !!x);

      return {
        navItems: navItemsPerResource.flat(),
        sites: sitesPerResource.flat(),
        selectedSiteId: {
          accountId: selectedSite.accountId,
          siteId: selectedSite.siteId || -1,
        },
      };
    } finally {
      this.closeCreatedConnections(resources);
    }
  }

  async refetchNavItems(): Promise<NavItem[]> {
    const resources = this.appSettings.navConfigResources.filter(x => x.resourceType === 'IFRAME');

    try {
      const navItemsPerResource = await Promise.all(resources.map(r => this.fetchNavItems(r)));

      return navItemsPerResource.flat().concat(this.navItemsFromRestCache);
    } finally {
      this.closeCreatedConnections(resources);
    }
  }

  private async fetchNavItems(resource: ConfigResource): Promise<NavItem[]> {
    switch (resource.resourceType) {
      case 'REST':
        this.logger.debug(`Fetching nav items from an REST endpoint '${resource.url}'`);
        const restNavItems = await this.fetchNavItemsFromREST(resource.url);
        this.logger.debug(`Nav items have been received from the REST endpoint '${resource.url}'`, restNavItems);

        return restNavItems;

      case 'INTERNAL_REST':
        this.logger.debug(`Fetching nav items from an Internal REST endpoint '${resource.url}'`);
        const internalRestNavItems = await this.fetchNavItemsFromInternalREST(resource.url);
        this.logger.debug(`Nav items have been received from the Internal REST endpoint '${resource.url}'`, internalRestNavItems);

        return internalRestNavItems;

      case 'IFRAME':
        this.logger.debug(`Fetching nav items from an iframe '${resource.url}'`);
        const iframeNavItems = await this.fetchFromIframe(resource.url, 'getNavItems');
        this.logger.debug(`Nav items have been received from the iframe '${resource.url}'`, iframeNavItems);

        return iframeNavItems;

      default:
        return Promise.reject(
          new Error(`Resource type ${resource.resourceType} is not supported`),
        );
    }
  }

  private async fetchSites(resource: ConfigResource): Promise<Site[]> {
    if (resource.resourceType !== 'IFRAME') {
      return [];
    }

    this.logger.debug(`Fetching sites from an iframe '${resource.url}'`);
    const sites = await this.fetchFromIframe(resource.url, 'getSites');
    this.logger.debug(`Sites have been received from the iframe '${resource.url}'`, sites);

    return sites;
  }

  private async fetchSelectedSite(resource: ConfigResource): Promise<SiteId> {
    if (resource.resourceType !== 'IFRAME') {
      return undefined;
    }

    this.logger.debug(`Fetching a selected site from an iframe '${resource.url}'`);
    const selectedSite = await this.fetchFromIframe(resource.url, 'getSelectedSite');
    this.logger.debug(`Selected site has been received from the iframe '${resource.url}'`, selectedSite);

    return selectedSite;
  }

  private fetchNavItemsFromREST(url: string): Promise<NavItem[]> {
    this.logger.debug(`Fetching configuration from an REST endpoint '${url}'`);

    return this.http.get<NavItem[]>(url).pipe(
      tap(x => this.logger.debug(`Nav items have been received from the REST endpoint '${url}'`, x)),
    ).toPromise();
  }

  private async fetchNavItemsFromInternalREST(url: string): Promise<NavItem[]> {
    this.logger.debug(`Fetching configuration from an Internal REST endpoint '${url}'`);

    const baseUrl = this.location.prepareExternalUrl(this.appSettings.basePath);
    url = Location.joinWithSlash(baseUrl, url);

    const navItems = await this.fetchNavItemsFromREST(url);
    navItems.forEach(item => item.appIframeUrl = Location.joinWithSlash(baseUrl, item.appIframeUrl));

    return navItems;
  }

  private async fetchFromIframe(url: string, method: ChildApiMethod<'getNavItems'>): Promise<NavItem[]>;
  private async fetchFromIframe(url: string, method: ChildApiMethod<'getSites'>): Promise<Site[]>;
  private async fetchFromIframe(url: string, method: ChildApiMethod<'getSelectedSite'>): Promise<SiteId>;
  private async fetchFromIframe(url: string, method: string): Promise<any> {
    const connection = await this.connectionService.createConnection(url);

    return connection.api[method]();
  }

  private closeCreatedConnections(resources: ConfigResource[]): void {
    const iframeResources = resources.filter(x => x.url === 'IFRAME');

    for (const resource of iframeResources) {
      try {
        this.connectionService.removeConnection(resource.url);
      } catch (e) {
        this.logger.error('Could not close connection to configuration provider iframe', e);
      }
    }
  }
}
