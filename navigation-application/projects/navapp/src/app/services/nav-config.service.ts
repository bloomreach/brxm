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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import {
  NavItem,
  Site,
  SiteId,
} from '@bloomreach/navapp-communication';

import { AppSettings } from '../models/dto/app-settings.dto';
import { ConfigResource } from '../models/dto/config-resource.dto';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';
import { NavItemService } from './nav-item.service';
import { SiteService } from './site.service';

interface Configuration {
  navItems: NavItem[];
  sites: Site[];
  selectedSiteId: SiteId;
}

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  constructor(
    private http: HttpClient,
    private location: Location,
    private connectionService: ConnectionService,
    private navItemService: NavItemService,
    private siteService: SiteService,
    @Inject(APP_SETTINGS) private appSettings: AppSettings,
  ) { }

  init(): Promise<void> {
    return this.fetchAndMergeConfigurations()
      .then(({ navItems, sites, selectedSiteId }) => {
        this.navItemService.navItems = navItems;
        this.siteService.sites = sites;

        if (selectedSiteId) {
          this.siteService.setSelectedSite(selectedSiteId);
        }
      });
  }

  private fetchAndMergeConfigurations(): Promise<Configuration> {
    const configurationPromises = this.appSettings.navConfigResources.map(
      resource => this.fetchConfiguration(resource),
    );

    return Promise.all(configurationPromises).then(configurations => this.mergeConfigurations(configurations));
  }

  private fetchConfiguration(resource: ConfigResource): Promise<Configuration> {
    switch (resource.resourceType) {
      case 'IFRAME':
        return this.connectionService
          .createConnection(resource.url)
          .then(connection => {
            const child = connection.api;
            const communications: Promise<any>[] = [];
            communications.push(
              child.getNavItems ? child.getNavItems() : Promise.resolve([]),
            );
            communications.push(
              child.getSites ? child.getSites() : Promise.resolve([]),
            );
            communications.push(
              child.getSelectedSite ? child.getSelectedSite() : Promise.resolve(undefined),
            );

            return Promise.all(communications).then(
              ([ navItems, sites, selectedSiteId ]) => ({
                navItems,
                sites,
                selectedSiteId,
              }),
            );
          })
          .finally(() => {
            this.connectionService.removeConnection(resource.url);
          });
      case 'REST':
        return this.fetchFromREST<NavItem[]>(resource.url).then(navItems => ({
          navItems,
          sites: [],
          selectedSiteId: undefined,
        }));
      case 'INTERNAL_REST':
        const baseUrl = this.location.prepareExternalUrl(this.appSettings.basePath);
        const url = Location.joinWithSlash(baseUrl, resource.url);

        return this.fetchFromREST<NavItem[]>(url).then(navItems => {
          navItems.forEach(item => item.appIframeUrl = Location.joinWithSlash(baseUrl, item.appIframeUrl));
          return {
            navItems,
            sites: [],
            selectedSiteId: undefined,
          };
        });
      default:
        return Promise.reject(
          new Error(`Resource type ${resource.resourceType} is not supported`),
        );
    }
  }

  private mergeConfigurations(configurations: Configuration[]): Configuration {
    const {
      mergedNavItems,
      mergedSites,
      selectedSiteId,
    } = configurations.reduce(
      (result, configuration) => {
        if (!configuration) {
          return result;
        }

        result.mergedNavItems = result.mergedNavItems.concat(
          configuration.navItems,
        );
        result.mergedSites = result.mergedSites.concat(configuration.sites);

        if (configuration.selectedSiteId) {
          result.selectedSiteId = {
            accountId: configuration.selectedSiteId.accountId,
            siteId: configuration.selectedSiteId.siteId || -1,
          };
        }

        return result;
      },
      { mergedNavItems: [], mergedSites: [], selectedSiteId: undefined },
    );

    return {
      navItems: mergedNavItems,
      sites: mergedSites,
      selectedSiteId,
    };
  }

  private fetchFromREST<T>(url: string): Promise<T> {
    return this.http.get<T>(url).toPromise();
  }
}
