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
  ChildConnectConfig, ChildPromisedApi,
  connectToChild,
} from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ConfigResource, NavItem, Site } from '../models';

import { GlobalSettingsService } from './global-settings.service';

const filterOutEmpty = items => !!(Array.isArray(items) && items.length);

interface Configuration {
  navItems: NavItem[];
  sites: Site[];
}

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  private readonly renderer: Renderer2;

  private navItems = new BehaviorSubject<NavItem[]>([]);
  private sites = new BehaviorSubject<Site[]>([]);

  constructor(
    private http: HttpClient,
    private settingsService: GlobalSettingsService,
    private rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document,
  ) {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);
  }

  get navItems$(): Observable<NavItem[]> {
    return this.navItems.asObservable().pipe(filter(filterOutEmpty));
  }

  get sites$(): Observable<Site[]> {
    return this.sites.asObservable().pipe(filter(filterOutEmpty));
  }

  init(): Promise<void> {
    const configurationPromises = this.settingsService.appSettings.navConfigResources.map(
      resource => this.fetchConfiguration(resource),
    );

    return Promise.all(configurationPromises).then(configurations => {
      const { mergedNavItems, mergedSites } = configurations.reduce((result, { navItems, sites }) => {
        result.mergedNavItems = result.mergedNavItems.concat(navItems);
        result.mergedSites = result.mergedSites.concat(sites);

        return result;
      }, { mergedNavItems: [], mergedSites: [] });

      this.navItems.next(mergedNavItems);
      this.sites.next(mergedSites);
    });
  }

  findNavItem(iframeUrl: string, path: string): NavItem {
    const navItems = this.navItems.value;

    return navItems.find(x => x.appIframeUrl === iframeUrl && x.appPath === path);
  }

  private fetchConfiguration(resource: ConfigResource): Promise<Configuration> {
    switch (resource.resourceType) {
      case 'IFRAME':
        return this.fetchFromIframe(resource.url, child => Promise.all([
          child.getNavItems(),
          child.getSites ? child.getSites() : Promise.resolve([]),
        ]).then(([navItems, sites]) => ({ navItems, sites })));
      case 'REST':
        return this.fetchFromREST<NavItem[]>(resource.url).then(navItems => ({ navItems, sites: [] }));
      default:
        return Promise.reject(
          new Error(`Resource type ${resource.resourceType} is not supported`),
        );
    }
  }

  private fetchFromIframe<T>(url: string, fetcher: (child: ChildPromisedApi) => Promise<T>): Promise<T> {
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
}
