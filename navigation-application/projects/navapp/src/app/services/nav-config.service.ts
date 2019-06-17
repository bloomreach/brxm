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

import { HttpClient } from '@angular/common/http';
import { Injectable, RendererFactory2 } from '@angular/core';
import {
  ChildConnectConfig,
  connectToChild,
} from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

import { NavConfigResource, NavItem } from '../models';

import { NavAppSettingsService } from './navapp-settings.service';

@Injectable({
  providedIn: 'root',
})
export class NavConfigService {
  private hostElement = document.body;
  private renderer = this.rendererFactory.createRenderer(undefined, undefined);

  private navItems = new BehaviorSubject<NavItem[]>([]);

  constructor(
    private http: HttpClient,
    private navAppSettings: NavAppSettingsService,
    private rendererFactory: RendererFactory2,
  ) {}

  get navItems$(): Observable<NavItem[]> {
    return this.navItems.asObservable().pipe(filter(items => items.length > 0));
  }

  init(): Promise<void> {
    const resourcePromises = this.navAppSettings.appSettings.navConfigResources.map(
      resource => this.fetchNavItems(resource),
    );

    return Promise.all(resourcePromises)
      .then(navItemArrays => [].concat(...navItemArrays))
      .then(navItems => this.navItems.next(navItems));
  }

  findNavItem(iframeUrl: string, path: string): NavItem {
    const navItems = this.navItems.value;

    return navItems.find(x => x.appIframeUrl === iframeUrl && x.appPath === path);
  }

  private fetchNavItems(resource: NavConfigResource): Promise<NavItem[]> {
    switch (resource.resourceType) {
      case 'IFRAME':
        return this.getItemsFromIframe(resource.url);
      case 'REST':
        return this.getItemsFromREST(resource.url);
      default:
        return Promise.reject(
          new Error(`Resource type ${resource.resourceType} is not supported`),
        );
    }
  }

  private getItemsFromREST(url: string): Promise<NavItem[]> {
    return this.http.get<NavItem[]>(url).toPromise();
  }

  private getItemsFromIframe(url: string): Promise<NavItem[]> {
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.style.visibility = 'hidden';
    iframe.style.position = 'absolute';
    this.renderer.appendChild(this.hostElement, iframe);

    const config: ChildConnectConfig = {
      iframe,
    };

    return connectToChild(config)
      .then(child => child.getNavItems())
      .finally(() => this.renderer.removeChild(this.hostElement, iframe));
  }
}
