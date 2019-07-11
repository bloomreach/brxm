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
} from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ConfigResource } from '../models/dto/config-resource.dto';
import { NavItem } from '../models/dto/nav-item.dto';
import { Site } from '../models/dto/site.dto';

import { GlobalSettingsService } from './global-settings.service';

const filterOutEmptyArray = items => !!(Array.isArray(items) && items.length);

interface Configuration {
  navItems: NavItem[];
  sites: Site[];
  selectedSiteId: number;
}

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  private readonly renderer: Renderer2;

  private navItems = new BehaviorSubject<NavItem[]>([]);
  private sites = new BehaviorSubject<Site[]>([]);
  private selectedSite = new BehaviorSubject(undefined);

  constructor(
    private http: HttpClient,
    private settingsService: GlobalSettingsService,
    private rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document,
  ) {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);
  }

  get navItems$(): Observable<NavItem[]> {
    return this.navItems.asObservable().pipe(filter(filterOutEmptyArray));
  }

  get sites$(): Observable<Site[]> {
    return this.sites.asObservable().pipe(filter(filterOutEmptyArray));
  }

  get selectedSite$(): Observable<Site> {
    return this.selectedSite.asObservable().pipe(filter(value => !!value));
  }

  init(): Promise<void> {
    const configurationPromises = this.settingsService.appSettings.navConfigResources.map(
      resource => this.fetchConfiguration(resource),
    );

    return Promise.all(configurationPromises).then(configurations => {
      const {
        mergedNavItems,
        mergedSites,
        selectedSiteId,
      } = configurations.reduce(
        (result, configuration) => {
          result.mergedNavItems = result.mergedNavItems.concat(
            configuration.navItems,
          );
          result.mergedSites = result.mergedSites.concat(configuration.sites);

          if (configuration.selectedSiteId !== undefined) {
            result.selectedSiteId = configuration.selectedSiteId;
          }

          return result;
        },
        { mergedNavItems: [], mergedSites: [], selectedSiteId: undefined },
      );

      const site = this.findSite(mergedSites, selectedSiteId) || mergedSites[0];

      this.navItems.next(mergedNavItems);
      this.sites.next(mergedSites);
      this.selectedSite.next(site);
    });
  }

  findNavItem(iframeUrl: string, path: string): NavItem {
    const navItems = this.navItems.value;

    return navItems.find(
      x => x.appIframeUrl === iframeUrl && x.appPath === path,
    );
  }

  private fetchConfiguration(resource: ConfigResource): Promise<Configuration> {
    switch (resource.resourceType) {
      case 'IFRAME':
        return this.fetchFromIframe(resource.url, child => {
          const communications: Promise<any>[] = [];
          communications.push(
            child.getNavItems ? child.getNavItems() : Promise.resolve(),
          );
          communications.push(
            child.getSites ? child.getSites() : Promise.resolve(),
          );
          communications.push(
            child.getSelectedSite ? child.getSelectedSite() : Promise.resolve(),
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
      default:
        return Promise.reject(
          new Error(`Resource type ${resource.resourceType} is not supported`),
        );
    }
  }

  private fetchFromIframe<T>(
    url: string,
    fetcher: (child: ChildPromisedApi) => Promise<T>,
  ): Promise<T> {
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.style.visibility = 'hidden';
    iframe.style.position = 'absolute';
    this.renderer.appendChild(this.document.body, iframe);

    const config: ChildConnectConfig = {
      iframe,
    };

    return connectToChild(config)
      .then(fetcher)
      .finally(() => this.renderer.removeChild(this.document.body, iframe));
  }

  private fetchFromREST<T>(url: string): Promise<T> {
    return this.http.get<T>(url).toPromise();
  }

  private findSite(sites: Site[], siteId: number): Site {
    for (const site of sites) {
      if (site.id === siteId) {
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
