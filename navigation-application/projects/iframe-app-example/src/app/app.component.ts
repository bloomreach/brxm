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

import { Location, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  connectToParent,
  NavLocation,
  ParentConnectConfig,
} from '@bloomreach/navapp-communication';
import { CookieService } from 'ngx-cookie-service';
import { ChildApi, ParentPromisedApi } from 'projects/navapp-communication/src/lib/api';

import { mockSites, navigationConfiguration } from './mocks';

const SITE_COOKIE_NAME = 'EXAMPLE_APP_SITE_ID';

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

  constructor(
    private location: Location,
    private cookiesService: CookieService,
  ) {}

  get isBrSmMock(): boolean {
    return this.location.path() === '/brsm';
  }

  get childApiMethods(): ChildApi {
    const methods: ChildApi = {
      navigate: (location: NavLocation) => {
        this.navigateCount += 1;
        this.navigatedTo = location.path;
      },
      getNavItems: () => {
        return navigationConfiguration;
      },
      logout: () => {
        if (this.navigateCount % 2) {
          return Promise.reject(new Error('Whoa!'));
        } else {
          return Promise.resolve();
        }
      },
    };

    if (this.isBrSmMock) {
      methods.getSites = () => {
        return mockSites;
      };

      methods.getSelectedSite = () => {
        return this.selectedSiteId;
      };

      methods.updateSite = (siteId?: number) => {
        this.selectedSiteId = siteId;
      };
    }

    return methods;
  }

  get selectedSiteId(): number {
    let siteId = +this.cookiesService.get(SITE_COOKIE_NAME);

    if (!siteId) {
      siteId = mockSites[0].id;
      this.selectedSiteId = siteId;
    }

    return siteId;
  }

  set selectedSiteId(value: number) {
    this.cookiesService.set(SITE_COOKIE_NAME, value.toString());
  }

  ngOnInit(): void {
    if (window.parent === window) {
      console.log('Iframe app was not loaded inside iframe');
      return;
    }

    const config: ParentConnectConfig = {
      parentOrigin: '*',
      methods: this.childApiMethods,
    };

    connectToParent(config).then(parent => (this.parent = parent));
  }

  onButtonClicked(): void {
    this.buttonClicked++;
  }

  toggleOverlay(): void {
    if (this.overlaid) {
      this.parent.hideMask().then(() => {
        console.log('hiding parent mask');
        this.overlaid = false;
      });
    } else {
      this.parent.showMask().then(() => {
        console.log('showing parent mask');
        this.overlaid = true;
      });
    }
  }

  navigateTo(path: string, breadcrumbLabel?: string): void {
    let onNavigatedPromise: Promise<void>;

    if (path.startsWith(this.navigatedTo)) {
      this.navigatedTo = path;
      onNavigatedPromise = this.parent.updateNavLocation({ path: this.navigatedTo, breadcrumbLabel });
    } else {
      onNavigatedPromise = this.parent.navigate({ path, breadcrumbLabel });
    }

    onNavigatedPromise.then(() => {
      console.log(`Successfully navigated to ${path} page.`);
    });
  }
}
