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

import { Injectable } from '@angular/core';
import { ChildApi, NavLocation, SiteId } from '@bloomreach/navapp-communication';

import { NavigationTrigger } from '../../../../navapp-communication/src/lib/api';
import navItemsPerSiteAndAccount from '../../assets/iframe-navitems-per-site-and-account.json';
import defaultNavItems from '../../assets/iframe-navitems.json';
import sites from '../../assets/sites.json';
import { environment } from '../../environments/environment';

import { AppState } from './app-state';

@Injectable({
  providedIn: 'root',
})
export class ChildApiMethodsService {
  constructor(private readonly state: AppState) { }

  getMethods(): ChildApi {
    let methods = this.getBasicMethods();

    if (this.state.isBrSmMock) {
      const brSmMockMethods = this.getBrSmMockMethods();

      methods = { ...methods, ...brSmMockMethods };
    }

    return methods;
  }

  private getBasicMethods(): ChildApi {
    return {
      getConfig: async () => ({
        apiVersion: this.state.navappCommunicationImplementationApiVersion,
        showSiteDropdown: false,
        communicationTimeout: 500,
      }),
      getNavItems: async () => {
        if (environment.generateErrorOnNavItemsLoading) {
          return Promise.reject(new Error('Unable to send nav items from an iframe app because it is disabled by the configuration'));
        }

        if (!this.state.selectedSiteId) {
          return defaultNavItems;
        }

        const key = `${this.state.selectedSiteId.siteId}-${this.state.selectedSiteId.accountId}`;
        const mockNavItemsPerSite = navItemsPerSiteAndAccount[key];

        return mockNavItemsPerSite || defaultNavItems;
      },
      navigate: async (location: NavLocation, triggeredBy: NavigationTrigger) => {
        this.state.navigateCount += 1;
        this.state.navigatedTo = location;
        this.state.lastNavigationTriggeredBy = triggeredBy;

        return new Promise(r => setTimeout(r, this.state.navigationDelay));
      },
      logout: async () => this.state.generateAnErrorUponLogout ?
        Promise.reject(new Error('Custom logout error')) :
        Promise.resolve(),
      onUserActivity: async () => { this.state.userActivityReported++; },
    };
  }

  private getBrSmMockMethods(): ChildApi {
    return {
      getConfig: async () => ({
        apiVersion: this.state.navappCommunicationImplementationApiVersion,
        showSiteDropdown: true,
      }),
      getSites: async () => sites,
      getSelectedSite: async () => this.state.selectedSiteId,
      updateSelectedSite: async (siteId?: SiteId) => { this.state.selectedSiteId = siteId; },
    };
  }
}
