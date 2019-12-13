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
import { Inject, Injectable } from '@angular/core';
import { NavItem, NavLocation } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services/client-app.service';
import { InternalError } from '../error-handling/models/internal-error';
import { stripOffQueryStringAndHash } from '../helpers/strip-off-query-string-and-hash';
import { AppSettings } from '../models/dto/app-settings.dto';

import { APP_SETTINGS } from './app-settings';
import { NavItemService } from './nav-item.service';

@Injectable({
  providedIn: 'root',
})
export class UrlMapperService {
  // Path parts without leading and trailing slashes
  private readonly pathPartsToStripOffFromIframeUrl: string[] = [
    'iframe',
    'sm',
  ];

  constructor(
    private readonly navItemService: NavItemService,
    private readonly clientAppService: ClientAppService,
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
  ) {
    this.pathPartsToStripOffFromIframeUrl.unshift(
      this.trimSlashes(this.basePath),
    );
  }

  get basePath(): string {
    return this.appSettings.basePath;
  }

  mapNavItemToBrowserUrl(navItem: NavItem): string {
    const appBasePath = this.normalizeAppIframeUrl(navItem.appIframeUrl);

    const path = Location.joinWithSlash(appBasePath, navItem.appPath);

    return Location.joinWithSlash(this.basePath, path);
  }

  mapNavLocationToBrowserUrl(navLocation: NavLocation, useCurrentApp = false): [string, NavItem] {
    if (useCurrentApp && !this.clientAppService.activeApp) {
      throw new InternalError('ERROR_INITIALIZATION', 'Active app is not set');
    }

    const iframeUrlOrPath = useCurrentApp ? this.clientAppService.activeApp.url : navLocation.pathPrefix;

    const navItem = this.navItemService.findNavItem(navLocation.path, iframeUrlOrPath);

    if (!navItem) {
      throw new Error('Nav item related to the provided Nav location is not found');
    }

    const baseBrowserUrl = this.mapNavItemToBrowserUrl(navItem);

    const appPathAddOn = navLocation.path.slice(navItem.appPath.length);
    const [
      appPathAddOnWithoutQueryStringAndHash,
      queryStringAndHash,
    ] = this.extractPathAndQueryStringAndHash(appPathAddOn);

    const browserUrl = Location.joinWithSlash(baseBrowserUrl, appPathAddOnWithoutQueryStringAndHash) + queryStringAndHash;

    return [browserUrl, navItem];
  }

  trimLeadingSlash(value: string): string {
    return value.replace(/^\//, '');
  }

  extractPathAndQueryStringAndHash(pathWithQueryStringAndHash: string): [string, string] {
    const pathWithoutQueryStringAndHash = stripOffQueryStringAndHash(pathWithQueryStringAndHash);
    const queryStringAndHash = pathWithQueryStringAndHash.slice(pathWithoutQueryStringAndHash.length);

    return [pathWithoutQueryStringAndHash, queryStringAndHash];
  }

  private trimSlashes(value: string): string {
    return this.trimLeadingSlash(value).replace(/\/$/, '');
  }

  private normalizeAppIframeUrl(appIframeUrl: string): string {
    let appBasePath: string;

    try {
      appBasePath = this.trimSlashes(new URL(appIframeUrl).pathname);
    } catch {
      throw new InternalError(undefined, `The url has incorrect format: ${appIframeUrl}`);
    }

    this.pathPartsToStripOffFromIframeUrl.forEach(pathPart => {
      const fullRegExp = new RegExp(`^${pathPart}$`);
      const prefixRegExp = new RegExp(`^${pathPart}\/`);

      appBasePath = appBasePath.replace(prefixRegExp, '').replace(fullRegExp, '');
    });

    return appBasePath;
  }
}
