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
import { NGXLogger } from 'ngx-logger';
import { map, tap } from 'rxjs/operators';

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
    private logger: NGXLogger,
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
      case 'REST':
        return this.fetchFromREST(resource.url);

      case 'INTERNAL_REST':
        return this.fetchFromInternalREST(resource.url);

      case 'IFRAME':
        return this.fetchFromIFrame(resource.url);

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

  private fetchFromREST(url: string): Promise<Configuration> {
    this.logger.debug(`Fetching configuration from an REST endpoint '${url}'`);

    return this.http.get<NavItem[]>(url).pipe(
      map(navItems => ({
        navItems,
        sites: [],
        selectedSiteId: undefined,
      })),
      tap(x => this.logger.debug(`Nav items have been received from the REST endpoint '${url}'`, x.navItems)),
    ).toPromise();
  }

  private fetchFromInternalREST(url: string): Promise<Configuration> {
    this.logger.debug(`Fetching configuration from an Internal REST endpoint '${url}'`);

    const baseUrl = this.location.prepareExternalUrl(this.appSettings.basePath);
    url = Location.joinWithSlash(baseUrl, url);

    return this.fetchFromREST(url).then(configuration => {
      configuration.navItems.forEach(item => item.appIframeUrl = Location.joinWithSlash(baseUrl, item.appIframeUrl));

      return configuration;
    });
  }

  private fetchFromIFrame(url: string): Promise<Configuration> {
    this.logger.debug(`Fetching configuration from an iframe '${url}'`);

    return this.connectionService
      .createConnection(url)
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
        ).then(x => {
          this.logger.debug(`Nav items have been received from the iframe '${url}'`, x.navItems);
          this.logger.debug(`Sites have been received from the iframe '${url}'`, x.sites);
          this.logger.debug(`Selected site id has been received from the iframe '${url}'`, x.selectedSiteId);

          return x;
        });
      })
      .finally(() => {
        this.connectionService.removeConnection(url);
      });
  }
}
