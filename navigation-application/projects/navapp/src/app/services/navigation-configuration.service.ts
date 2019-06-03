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
import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { ChildConnectConfig, connectToChild } from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

import { NavConfigResource, NavItem } from '../models';

import { NavAppSettingsService } from './navapp-settings.service';

@Injectable({
  providedIn: 'root',
})
export class NavigationConfigurationService {
  private readonly hostElement: HTMLElement;
  private readonly renderer: Renderer2;

  private navigationConfiguration = new BehaviorSubject<Map<string, NavItem>>(new Map());

  constructor(
    private http: HttpClient,
    private navAppSettings: NavAppSettingsService,
    private rendererFactory: RendererFactory2,
  ) {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);
    this.hostElement = document.body;
  }

  get navigationConfiguration$(): Observable<Map<string, NavItem>> {
    return this.navigationConfiguration.asObservable().pipe(filter(config => config.size > 0));
  }

  init(): Promise<void> {
    const resourcePromises = this.navAppSettings.appSettings.navConfigResources.map(resource =>
      this.fetchNavConfig(resource),
    );

    return Promise.all(resourcePromises)
      .then(navItemArrays => [].concat(...navItemArrays))
      .then(navItems => this.setNavigationConfiguration(navItems));
  }

  private setNavigationConfiguration(navItems: NavItem[]): void {
    const navItemMap = navItems.reduce(
      (configMap, item) => configMap.set(item.id, item),
      new Map<string, NavItem>(),
    );

    this.navigationConfiguration.next(navItemMap);
  }

  private fetchNavConfig(resource: NavConfigResource): Promise<NavItem[]> {
    if (resource.resourceType === 'IFRAME') {
      return this.getConfigFromIframe(resource.url);
    } else if (resource.resourceType === 'REST') {
      return this.getConfigFromREST(resource.url);
    } else {
      throw new Error(`Resource type ${resource.resourceType} is not supported`);
    }
  }

  private getConfigFromREST(url: string): Promise<NavItem[]> {
    return this.http.get<NavItem[]>(url).toPromise();
  }

  private getConfigFromIframe(url: string): Promise<NavItem[]> {
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
