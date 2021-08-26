/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { Injectable } from '@angular/core';
import { NavigationTrigger, NavLocation, SiteId } from '@bloomreach/navapp-communication';
import { CookieService } from 'ngx-cookie-service';

import sites from '../../assets/sites.json';

const SITE_COOKIE_NAME = 'EXAMPLE_APP_SITE_ID';

@Injectable({
  providedIn: 'root',
})
export class AppState {
  navappCommunicationImplementationApiVersion = '1.0.0';
  navigateCount = 0;
  navigationDelay = 300;
  navigatedTo: NavLocation;
  lastNavigationTriggeredBy: NavigationTrigger;
  buttonClickedCounter = 0;
  overlaid = false;
  userActivityReported = 0;
  historyPushStateCount = 0;
  historyReplaceStateCount = 0;
  generateAnErrorUponLogout = false;
  shouldAskBeforeNavigation = false;

  constructor(
    private readonly location: Location,
    private readonly cookiesService: CookieService,
  ) {}

  get isBrSmMock(): boolean {
    return this.location.path().startsWith('/brsm');
  }

  get selectedSiteId(): SiteId {
    let [accountId, siteId] = this.cookiesService.get(SITE_COOKIE_NAME).split(',').map(x => +x);

    if (!accountId || !siteId) {
      const firstSite = sites[0];

      accountId = firstSite.accountId;
      siteId = firstSite.siteId;

      this.selectedSiteId = { accountId, siteId };
    }

    return { accountId, siteId };
  }

  set selectedSiteId(value: SiteId) {
    this.cookiesService.set(SITE_COOKIE_NAME, `${value.accountId},${value.siteId}`);
  }
}
