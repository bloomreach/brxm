/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Observable, ReplaySubject } from 'rxjs';

import { ClientAppService } from '../client-app/services/client-app.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BusyIndicatorService } from './busy-indicator.service';

@Injectable({
  providedIn: 'root',
})
export class SiteService {
  private readonly selectedSite = new ReplaySubject<Site>(1);

  private currentSites: Site[] = [];

  constructor(
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly clientAppService: ClientAppService,
    private readonly windowRef: WindowRef,
    private readonly logger: NGXLogger,
  ) { }

  get selectedSite$(): Observable<Site> {
    return this.selectedSite.asObservable();
  }

  get sites(): Site[] {
    return this.currentSites;
  }

  init(sites: Site[], selectedSiteId: SiteId): void {
    this.currentSites = sites;

    const selectedSite = this.findSite(this.currentSites, selectedSiteId);
    this.selectedSite.next(selectedSite);
  }

  async updateSelectedSite(site: Site): Promise<void> {
    this.busyIndicatorService.show();

    if (!site.isNavappEnabled) {
      return this.redirectToiUI();
    }

    this.logger.debug(`updateSelectedSite() is called for the active app '${this.clientAppService.activeApp.url}'`, site);
    await this.clientAppService.activeApp.api.updateSelectedSite(site);
    this.logger.debug('Active app successfully updated the selected site');

    this.selectedSite.next(site);

    this.busyIndicatorService.hide();
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

  private redirectToiUI(): void {
    // For iUI mode, remove '/navapp' from the url.
    // This needs to be updated once we figure out a reasonable route name for navapp mode.
    const path = this.windowRef.nativeWindow.location.href;
    const newPath = path.replace('/navapp', '');

    this.windowRef.nativeWindow.location.assign(newPath);
  }
}
