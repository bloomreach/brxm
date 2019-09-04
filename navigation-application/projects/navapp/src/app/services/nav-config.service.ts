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

import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import {
  ChildConnectConfig,
  ChildPromisedApi,
  connectToChild,
  NavItem,
  Site,
  SiteId,
} from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ConfigResource } from '../models/dto/config-resource.dto';

import { GlobalSettingsService } from './global-settings.service';

interface Configuration {
  navItems: NavItem[];
  sites: Site[];
  selectedSiteId: SiteId;
}

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  private readonly renderer: Renderer2;

  private currentNavItems: NavItem[] = [];
  private currentSites: Site[] = [];
  private selectedSite = new BehaviorSubject(undefined);

  constructor(
    private http: HttpClient,
    private settings: GlobalSettingsService,
    private rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document,
  ) {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);
  }

  get navItems(): NavItem[] {
    return this.currentNavItems;
  }

  get sites(): Site[] {
    return this.currentSites;
  }

  get selectedSite$(): Observable<Site> {
    return this.selectedSite.asObservable().pipe(filter(value => !!value));
  }

  init(): Promise<void> {
    return this.loginIfNecessary()
      .then(() => this.fetchAndMergeConfigurations())
      .then(({ navItems, sites, selectedSiteId }) => {
        this.currentNavItems = navItems;
        this.currentSites = sites;

        if (selectedSiteId) {
          const selectedSite = this.findSite(sites, selectedSiteId);
          this.setSelectedSite(selectedSite);
        }
      });
  }

  logout(): Promise<void[]> {
    const logoutPromises = this.settings.appSettings.logoutResources.map(resource => this.logoutSilently(resource));
    return Promise.all(logoutPromises);
  }

  findNavItem(iframeUrl: string, path: string): NavItem {
    return this.currentNavItems.find(x => x.appIframeUrl === iframeUrl && path.startsWith(x.appPath));
  }

  setSelectedSite(site: Site): void {
    this.selectedSite.next(site);
  }

  private loginIfNecessary(): Promise<void> {
    const loginPromises = this.settings.appSettings.loginResources.map(
      resource => this.loginSilently(resource),
    );

    return Promise.all(loginPromises).then(results => {
      if (results.includes(false)) {
        // At least one iframe failed to login
        // For now just throw an error, will be handled properly when we implement error handling and timeouts
        throw new Error('failed to login');
      }
    });
  }

  private loginSilently(resource: string): Promise<boolean> {
    return this.fetchFromIframe(resource, api => Promise.resolve(api)).then(api => !!api);
  }

  private logoutSilently(resource: string): Promise<void> {
    return this.fetchFromIframe(resource, () => Promise.resolve());
  }

  private fetchAndMergeConfigurations(): Promise<Configuration> {
    const configurationPromises = this.settings.appSettings.navConfigResources.map(
      resource => this.fetchConfiguration(resource),
    );

    return Promise.all(configurationPromises).then(configurations => this.mergeConfigurations(configurations));
  }

  private fetchConfiguration(resource: ConfigResource): Promise<Configuration> {
    switch (resource.resourceType) {
      case 'IFRAME':
        return this.fetchFromIframe(resource.url, child => {
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
            ([navItems, sites, selectedSiteId]) => ({
              navItems,
              sites,
              selectedSiteId,
            }),
          );
        });
      case 'REST':
        return this.fetchFromREST<NavItem[]>(resource.url).then(navItems => ({
          navItems,
          sites: [],
          selectedSiteId: undefined,
        }));
      case 'INTERNAL_REST':
        const basePath = this.settings.appSettings.navAppBasePath;
        return this.fetchFromREST<NavItem[]>(basePath + resource.url).then(navItems => {
          navItems.forEach(item => item.appIframeUrl = basePath + item.appIframeUrl);
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

  private fetchFromIframe<T>(
    url: string,
    fetcher: (child: ChildPromisedApi) => Promise<T>,
  ): Promise<T> {
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.style.visibility = 'hidden';
    iframe.style.position = 'absolute';
    iframe.style.width = '1px';
    iframe.style.height = '1px';
    this.renderer.appendChild(this.document.body, iframe);

    const config: ChildConnectConfig = {
      iframe,
      timeout: this.settings.appSettings.iframesConnectionTimeout,
    };

    return connectToChild(config)
      .then(fetcher, () => undefined)
      .finally(() => this.renderer.removeChild(this.document.body, iframe));
  }

  private fetchFromREST<T>(url: string): Promise<T> {
    return this.http.get<T>(url).toPromise();
  }

  private findSite(sites: Site[], siteId: SiteId): Site {
    for (const site of sites) {
      if (site.accountId === siteId.accountId && site.siteId === siteId.siteId) {
        return site;
      }

      let childSite: Site;

      if (site.subGroups) {
        childSite = this.findSite(site.subGroups, siteId);
      }

      if (childSite) {
        return childSite;
      }
    }
  }
}
