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

import { Injectable } from '@angular/core';
import { NavItem, NavLocation } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services/client-app.service';
import { GlobalSettingsService } from '../services/global-settings.service';
import { NavConfigService } from '../services/nav-config.service';

const pathPartsToStripOffFromIframeUrl = [
  'cms',
  'iframe',
];

@Injectable({
  providedIn: 'root',
})
export class UrlMapperService {
  constructor(
    private settings: GlobalSettingsService,
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
  ) {}

  mapNavItemToBrowserUrl(navItem: NavItem): string {
    const contextPath = this.trimSlashes(this.settings.appSettings.contextPath);
    const appBasePath = this.normalizeAppIframeUrl(navItem.appIframeUrl);

    return this.combinePathParts(contextPath, appBasePath, navItem.appPath);
  }

  mapNavLocationToBrowserUrl(navLocation: NavLocation, useCurrentApp = false): [string, NavItem] {
    const activeAppUrl = this.clientAppService.activeApp.url;

    const appPathPredicate = (x: NavItem) => navLocation.path.startsWith(x.appPath);
    const appUrlAndAppPathPredicate = (x: NavItem) => x.appIframeUrl === activeAppUrl &&  appPathPredicate(x);

    const navItem = this.navConfigService.navItems.find(x => {
      return useCurrentApp ? appUrlAndAppPathPredicate(x) : appPathPredicate(x);
    });

    if (!navItem) {
      throw new Error('Nav item related to provided Nav location is not found');
    }

    const browserUrl = this.mapNavItemToBrowserUrl(navItem);
    const addPathAddOn = navLocation.path.slice(navItem.appPath.length);

    return [this.combinePathParts(browserUrl, addPathAddOn), navItem];
  }

  combinePathParts(...parts: string[]): string {
    const url = parts.filter(x => x.length > 0).map(x => this.trimSlashes(x)).join('/');

    return `/${this.trimLeadingSlash(url)}`;
  }

  trimLeadingSlash(value: string): string {
    return value.replace(/^\//, '');
  }

  private trimSlashes(value: string): string {
    return this.trimLeadingSlash(value).replace(/\/$/, '');
  }

  private normalizeAppIframeUrl(appIframeUrl: string): string {
    let appBasePath = this.trimSlashes(new URL(appIframeUrl).pathname);

    pathPartsToStripOffFromIframeUrl.forEach(pathPart => {
      const fullRegExp = new RegExp(`^${pathPart}$`);
      const prefixRegExp = new RegExp(`^${pathPart}\/`);

      appBasePath = appBasePath.replace(prefixRegExp, '').replace(fullRegExp, '');
    });

    return appBasePath;
  }
}
