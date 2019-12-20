/*!
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

import { Injectable, OnDestroy } from '@angular/core';
import { NavItem as NavItemDto } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { NavItem } from '../models/nav-item.model';

type PathActivationsMap = Map<string, Subject<boolean>>;

@Injectable({
  providedIn: 'root',
})
export class NavItemService implements OnDestroy {
  private sortedNavItems: NavItem[] = [];
  // map per app per path
  // app1
  //   path1
  //   path2
  // app2
  //   path1
  //   path2
  // etc
  private navItemActivators = new Map<string, PathActivationsMap>();
  private readonly unsubscribe = new Subject();

  constructor(private readonly logger: NGXLogger) {}

  get navItems(): NavItem[] {
    return this.sortedNavItems;
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  registerNavItemDtos(navItemDtos: NavItemDto[]): NavItem[] {
    this.navItemActivators = new Map<string, PathActivationsMap>();

    const navItems = navItemDtos.slice().map(dto => {
      const activation$ = this.createActivationObservable(dto).pipe(
        takeUntil(this.unsubscribe),
      );

      return new NavItem(dto, activation$);
    });

    navItems.sort((a, b) => b.appPath.length - a.appPath.length);

    this.sortedNavItems = navItems;

    return navItems;
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

    return this.navItems.find(x => iframeUrlOrPath ? iframeUrlPredicate(x) && appPathPredicate(x) : appPathPredicate(x));
  }

  activateNavItems(appIframeUrl: string): void {
    const appNavItemActivators = this.navItemActivators.get(appIframeUrl);

    if (!appNavItemActivators) {
      this.logger.warn(`An attempt to activate nav items for '${appIframeUrl}' has failed because there is no nap items registered for that app`);
      return;
    }

    this.logger.debug(`Activation of nav items for '${appIframeUrl}'`);

    appNavItemActivators.forEach(x => {
      x.next(true);
      x.complete();
    });
  }

  private createActivationObservable(navItemDto: NavItemDto): Observable<boolean> {
    const activationSubject = new ReplaySubject<boolean>(1);

    if (!this.navItemActivators.has(navItemDto.appIframeUrl)) {
      this.navItemActivators.set(navItemDto.appIframeUrl, new Map<string, Subject<boolean>>());
    }

    const appPathActivations = this.navItemActivators.get(navItemDto.appIframeUrl);
    appPathActivations.set(navItemDto.appPath, activationSubject);

    return activationSubject;
  }

}
