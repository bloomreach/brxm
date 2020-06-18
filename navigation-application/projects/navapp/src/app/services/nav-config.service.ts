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
import { NavItem, Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { AppSettings } from '../models/dto/app-settings.dto';
import { ConfigResource } from '../models/dto/config-resource.dto';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';

export interface Configuration {
  navItems: NavItem[];
  sites: Site[];
  selectedSiteId: SiteId;
}

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  private static isValidAbsoluteUrl(url: string): boolean {
    try {
      return !! new URL(url);
    } catch {
      return false;
    }
  }

  constructor(
    private readonly http: HttpClient,
    private readonly location: Location,
    private readonly connectionService: ConnectionService,
    private readonly logger: NGXLogger,
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
  ) { }

  async fetchNavigationConfiguration(): Promise<Configuration> {
    const resources = this.appSettings.navConfigResources;

    try {
      const [navItemsPerResource, sitesPerResource, selectedSitePerResource] = await Promise.all([
        Promise.all(resources.map(r => this.fetchNavItems(r))),
        Promise.all(resources.map(r => this.fetchSitesFromIframe(r))),
        Promise.all(resources.map(r => this.fetchSelectedSiteFromIframe(r))),
      ]);

      return {
        navItems: navItemsPerResource.flat(),
        sites: sitesPerResource.flat(),
        selectedSiteId: this.findSelectedSiteId(selectedSitePerResource),
      };
    } finally {
      this.closeCreatedConnections(resources);
    }
  }

  async refetchNavItems(): Promise<NavItem[]> {
    const resources = this.appSettings.navConfigResources;

    try {
      const navItemsPerResource = await Promise.all(resources.map(r => this.fetchNavItems(r)));

      return navItemsPerResource.flat();
    } finally {
      this.closeCreatedConnections(resources);
    }
  }

  private async fetchNavItems(resource: ConfigResource): Promise<NavItem[]> {
    switch (resource.resourceType) {
      case 'REST':
      case 'INTERNAL_REST':
        return this.fetchNavItemsFromREST(resource.url);

      case 'IFRAME':
        return this.fetchNavItemsFromIframe(resource.url);

      default:
        return Promise.reject(
          new Error(`Resource type ${resource.resourceType} is not supported`),
        );
    }
  }

  private async fetchSitesFromIframe(resource: ConfigResource): Promise<Site[]> {
    if (resource.resourceType !== 'IFRAME') {
      return [];
    }

    try {
      this.logger.debug(`Fetching sites from an iframe '${resource.url}'`);
      const api = await this.connectionService.connect(resource.url);
      const sites = await api.getSites();
      this.logger.debug(`Sites have been fetched from the iframe '${resource.url}'`, sites);

      return sites;
    } catch (e) {
      this.logger.error(`Unable to fetch sites from the iframe '${resource.url}'`, e);

      return [];
    }
  }

  private async fetchSelectedSiteFromIframe(resource: ConfigResource): Promise<SiteId> {
    if (resource.resourceType !== 'IFRAME') {
      return undefined;
    }

    try {
      this.logger.debug(`Fetching a selected site from an iframe '${resource.url}'`);
      const api = await this.connectionService.connect(resource.url);
      const selectedSite = await api.getSelectedSite();
      this.logger.debug(`Selected site has been fetched from the iframe '${resource.url}'`, selectedSite);

      return selectedSite;
    } catch (e) {
      this.logger.error(`Unable to fetch a selected site from the iframe '${resource.url}'`, e);
    }
  }

  private async fetchNavItemsFromREST(url: string): Promise<NavItem[]> {
    try {
      url = this.ensureUrlIsAbsolute(url);

      this.logger.debug(`Fetching nav items from an REST endpoint '${url}'`);
      const navItems = await this.http.get<NavItem[]>(url).toPromise();
      this.logger.debug(`Nav items have been fetched from the REST endpoint '${url}'`, navItems);

      const normalizedNavItems = this.normalizeNavItems(navItems);
      this.logger.debug(`Nav items fetched from the REST endpoint '${url}' after normalization`, normalizedNavItems);

      return normalizedNavItems;
    } catch (e) {
      this.logger.error(`Unable to fetch nav items from the REST endpoint '${url}'`, e.message);

      return [];
    }
  }

  private async fetchNavItemsFromIframe(url: string): Promise<NavItem[]> {
    try {
      this.logger.debug(`Fetching nav items from an iframe '${url}'`);
      const api = await this.connectionService.connect(url);
      const navItems = await api.getNavItems();
      this.logger.debug(`Nav items have been fetched from the iframe '${url}'`, navItems);

      return navItems;
    } catch (e) {
      this.logger.error(`Unable to fetch nav items from an iframe '${url}'`, e);

      return [];
    }
  }

  private closeCreatedConnections(resources: ConfigResource[]): void {
    const iframeResources = resources.filter(x => x.resourceType === 'IFRAME');

    for (const resource of iframeResources) {
      try {
        this.connectionService.disconnect(resource.url);
      } catch (e) {
        this.logger.error('Could not close connection to iframe configuration provider', e);
      }
    }
  }

  private findSelectedSiteId(selectedSitesFromResources: SiteId[]): SiteId {
    const selectedSite = selectedSitesFromResources.find(x => !!x);

    if (!selectedSite) {
      return;
    }

    return {
      accountId: selectedSite.accountId,
      siteId: selectedSite.siteId || -1,
    };
  }

  private ensureUrlIsAbsolute(url: string): string {
    if (NavConfigService.isValidAbsoluteUrl(url)) {
      return url;
    }

    const baseUrl = this.location.prepareExternalUrl(this.appSettings.basePath);

    return Location.joinWithSlash(baseUrl, url);
  }

  private normalizeNavItems(navItems: NavItem[]): NavItem[] {
    return navItems.map(navItem => ({
      ...navItem,
      appIframeUrl: this.ensureUrlIsAbsolute(navItem.appIframeUrl),
    }));
  }
}
