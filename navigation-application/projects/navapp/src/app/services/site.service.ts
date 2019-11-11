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

import { Injectable } from '@angular/core';
import { Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Observable, ReplaySubject } from 'rxjs';

import { ClientAppService } from '../client-app/services/client-app.service';

import { BusyIndicatorService } from './busy-indicator.service';

@Injectable({
  providedIn: 'root',
})
export class SiteService {
  sites: Site[] = [];
  private selectedSite = new ReplaySubject<Site>(1);

  constructor(
    private busyIndicatorService: BusyIndicatorService,
    private clientAppService: ClientAppService,
    private logger: NGXLogger,
  ) { }

  get selectedSite$(): Observable<Site> {
    return this.selectedSite.asObservable();
  }

  setSelectedSite(siteId: SiteId): void {
    const selectedSite = this.findSite(this.sites, siteId);
    this.selectedSite.next(selectedSite);
  }

  updateSelectedSite(siteId: SiteId): Promise<void> {
    this.busyIndicatorService.show();

    this.logger.debug(`updateSelectedSite() is called for the active app '${this.clientAppService.activeApp.url}'`, siteId);

    return this.clientAppService.activeApp.api
      .updateSelectedSite(siteId)
      .then(() => {
        this.logger.debug('Active app successfully updated the selected site. Start broadcasting updateSelectedSite() for the other apps.');

        const updatePromises = this.clientAppService.apps
          .filter(app => app.api && app.api.updateSelectedSite && app !== this.clientAppService.activeApp)
          .map(app => {
            this.logger.debug(`updateSelectedSite() is called for '${app.url}'`);

            return app.api.updateSelectedSite();
          });

        return Promise.all(updatePromises).then(() => {
          this.logger.debug('updateSelectedSite() broadcasting finished successfully');
        });
      })
      .then(() => this.busyIndicatorService.hide());
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
}
