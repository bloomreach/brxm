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
import { filter, map, tap, withLatestFrom } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';

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
    private readonly connectionService: ConnectionService,
  ) {
    this.connectionService.updateSelectedSite$
      .pipe(
        withLatestFrom(this.selectedSite$.pipe(map(({siteId, accountId}) => ({siteId, accountId})))),
        filter(([newSiteId, selectedSiteId]) => !this.areSiteIdsEqual(newSiteId, selectedSiteId)),
      ).subscribe(([newSiteId]) => this.selectSite(newSiteId));
  }

  get selectedSite$(): Observable<Site> {
    return this.selectedSite.asObservable();
  }

  get sites(): Site[] {
    return this.currentSites;
  }

  init(sites: Site[], selectedSiteId: SiteId): void {
    this.currentSites = sites;

    this.selectSite(selectedSiteId);
  }

  async updateSelectedSite(site: Site): Promise<void> {
    this.busyIndicatorService.show();

    this.logger.debug(`updateSelectedSite() is called for the active app '${this.clientAppService.activeApp.url}'`, site);
    await this.clientAppService.activeApp.api.updateSelectedSite(site);
    this.logger.debug('Active app successfully updated the selected site');

    if (!site.isNavappEnabled) {
      this.logger.debug('Redirect to SM iUI for non-navapp site/account');
      return this.redirectToiUI();
    }

    this.selectedSite.next(site);

    this.busyIndicatorService.hide();
  }

  private selectSite(siteId: SiteId): void {
    const selectedSite = this.findSite(this.currentSites, siteId);
    this.selectedSite.next(selectedSite);
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

    let newUrl = new URL(path.replace('/navapp', ''));
    // if no app URL specified, set to current active app's url
    if (newUrl.pathname === '/') {
      const currentAppUrl = new URL(this.clientAppService.activeApp.url);
      newUrl = new URL(newUrl.href + currentAppUrl.pathname.replace('/sm/', ''));
    }

    this.windowRef.nativeWindow.location.assign(newUrl.href);
  }

  private areSiteIdsEqual(siteId1: SiteId, siteId2: SiteId): boolean {
    return (siteId1.accountId === siteId2.accountId) && (siteId1.siteId === siteId2.siteId);
  }
}
