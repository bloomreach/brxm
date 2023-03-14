/*!
 * Copyright 2021-2023 Bloomreach
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

import { Injectable, OnDestroy } from '@angular/core';
import { NavItem } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class NavItemService {
  private sortedNavItems: NavItem[] = [];
  private navItems: NavItem[];

  constructor(private readonly logger: NGXLogger) {}

  registerNavItems(navItems: NavItem[]): NavItem[] {
    this.logger.debug('Register nav items', navItems);

    this.sortedNavItems = [...navItems].sort((a, b) => b.appPath.length - a.appPath.length);
    this.navItems = navItems;
    return this.navItems;
  }

  findNavItem(path: string, iframeUrlOrPath?: string): NavItem {
    let isIframeUrl = false;

    try {
      isIframeUrl = !!new URL(iframeUrlOrPath).pathname;
    } catch {}

    const appPathPredicate = (x: NavItem) => path.startsWith(x.appPath);
    const iframeUrlPredicate = (x: NavItem) => {
      if (isIframeUrl) {
        return x.appIframeUrl === iframeUrlOrPath;
      }

      try {
        return new URL(x.appIframeUrl).pathname === iframeUrlOrPath;
      } catch {
        this.logger.warn(`Unable to parse nav items's url "${x.appIframeUrl}"`);
        return false;
      }
    };

    return this.sortedNavItems.find(x => iframeUrlOrPath ? iframeUrlPredicate(x) && appPathPredicate(x) : appPathPredicate(x));
  }
}
