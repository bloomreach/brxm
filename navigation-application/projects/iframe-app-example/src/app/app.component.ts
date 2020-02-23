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

// tslint:disable:no-console
import { Location, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  ChildApi,
  connectToParent,
  NavLocation,
  ParentConnectConfig,
  ParentPromisedApi,
  SiteId,
} from '@bloomreach/navapp-communication';
import { CookieService } from 'ngx-cookie-service';

import { mockNavItems, mockNavItemsMapPerSite, mockSites } from './mocks';

const SITE_COOKIE_NAME = 'EXAMPLE_APP_SITE_ID';
const NAVAPP_COMMUNICATION_IMPLEMENTATION_API_VERSION = '1.0.0';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  providers: [
    Location,
    { provide: LocationStrategy, useClass: PathLocationStrategy },
  ],
})
export class AppComponent implements OnInit {
  navigateCount = 0;
  navigatedTo: string;
  buttonClicked = 0;
  parent: ParentPromisedApi;
  overlaid = false;
  parentApiVersion: string;

  constructor(
    private readonly location: Location,
    private readonly cookiesService: CookieService,
  ) { }

  get isBrSmMock(): boolean {
    return this.location.path() === '/brsm';
  }

  get childApiMethods(): ChildApi {
    const methods: ChildApi = {
      getConfig: () => ({
        apiVersion: NAVAPP_COMMUNICATION_IMPLEMENTATION_API_VERSION,
        showSiteDropdown: false,
        communicationTimeout: 500,
      }),
      navigate: (location: NavLocation) => {
        this.navigateCount += 1;
        this.navigatedTo = location.path;

        if (this.navigatedTo === 'experience-manager/channel1') {
          this.parent.updateNavLocation({
            path: location.path,
            breadcrumbLabel: 'Channel 1',
          });
        }

        return new Promise(r => setTimeout(r, 300));
      },
      getNavItems: () => {
        if (!this.selectedSiteId) {
          return mockNavItems;
        }

        const key = `${this.selectedSiteId.siteId}${this.selectedSiteId.accountId}`;
        const mockNavItemsPerSite = mockNavItemsMapPerSite[key];

        return mockNavItemsPerSite || mockNavItems;
      },
      logout: () => {
        if (this.navigateCount % 2) {
          return Promise.reject(new Error('Whoa!'));
        } else {
          return Promise.resolve();
        }
      },
      onUserActivity: () => {
        console.log('parent reported user activity');
      },
    };

    if (this.isBrSmMock) {
      methods.getConfig = () => ({
        apiVersion: NAVAPP_COMMUNICATION_IMPLEMENTATION_API_VERSION,
        showSiteDropdown: true,
      });

      methods.getSites = () => {
        return mockSites;
      };

      methods.getSelectedSite = () => {
        return this.selectedSiteId;
      };

      methods.updateSelectedSite = (siteId?: SiteId) => {
        this.selectedSiteId = siteId;
      };
    }

    return methods;
  }

  get selectedSiteId(): SiteId {
    let [accountId, siteId] = this.cookiesService.get(SITE_COOKIE_NAME).split(',').map(x => +x);

    if (!accountId || !siteId) {
      const firstSite = mockSites[0];

      accountId = firstSite.accountId;
      siteId = firstSite.siteId;

      this.selectedSiteId = { accountId, siteId };
    }

    return { accountId, siteId };
  }

  set selectedSiteId(value: SiteId) {
    this.cookiesService.set(SITE_COOKIE_NAME, `${value.accountId},${value.siteId}`);
  }

  ngOnInit(): void {
    if (window.parent === window) {
      console.error('Iframe app was not loaded inside iframe');
      return;
    }

    const config: ParentConnectConfig = {
      parentOrigin: '*',
      methods: this.childApiMethods,
    };

    connectToParent(config)
      .then(parent => (this.parent = parent))
      .then(() => {
        if (this.parent.getConfig) {
          this.parent.getConfig()
            .then(parentConfig => this.parentApiVersion = parentConfig.apiVersion);
        }
      });
  }

  onButtonClicked(): void {
    this.parent.onUserActivity().then(() => { this.buttonClicked++; });
  }

  toggleOverlay(): void {
    if (this.overlaid) {
      this.parent.hideMask().then(() => {
        this.overlaid = false;
      });
    } else {
      this.parent.showMask().then(() => {
        this.overlaid = true;
      });
    }
  }

  navigateTo(path: string, breadcrumbLabel?: string): void {
    if (path.startsWith(this.navigatedTo)) {
      this.navigatedTo = path;
      this.parent.updateNavLocation({ path: this.navigatedTo, breadcrumbLabel });
    } else {
      this.parent.navigate({ path, breadcrumbLabel });
    }
  }

  showError(): void {
    this.parent.onError({
      errorCode: 500,
      message: 'Error from the iframe app example',
    });
  }
}
