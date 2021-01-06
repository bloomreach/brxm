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

import { fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { ChildApi, NavigationTrigger, NavItem, NavLocation, Site } from '@bloomreach/navapp-communication';

import { environment } from '../../environments/environment';

import { AppState } from './app-state';
import { ChildApiMethodsService } from './child-api-methods.service';

describe('ChildApiMethodsService', () => {
  let childApiMethods: ChildApi;
  let stateMock: AppState;

  beforeEach(() => {
    stateMock = {
      navappCommunicationImplementationApiVersion: '1.2.0',
      navigationDelay: 300,
      navigateCount: 0,
      navigatedTo: undefined,
      lastNavigationTriggeredBy: undefined,
      userActivityReported: 0,
      generateAnErrorUponLogout: false,
      isBrSmMock: false,
      selectedSiteId: undefined,
    } as any;

    TestBed.configureTestingModule({
      providers: [
        ChildApiMethodsService,
        { provide: AppState, useValue: stateMock },
      ],
    });

    const service: ChildApiMethodsService = TestBed.get(ChildApiMethodsService);
    stateMock = TestBed.get(AppState);

    childApiMethods = service.getMethods();
  });

  it('should return proper methods', () => {
    expect(childApiMethods.getConfig).toBeDefined();
    expect(childApiMethods.navigate).toBeDefined();
    expect(childApiMethods.getNavItems).toBeDefined();
    expect(childApiMethods.logout).toBeDefined();
    expect(childApiMethods.onUserActivity).toBeDefined();
  });

  describe('getConfig()', () => {
    it('should return child config', async () => {
      const expected = {
        apiVersion: '1.2.0',
        showSiteDropdown: false,
        communicationTimeout: 500,
      };

      const actual = await childApiMethods.getConfig();

      expect(actual).toEqual(expected);
    });
  });

  describe('getNavItems()', () => {
    describe('if environment.generateErrorOnNavItemsLoading is true', () => {
      beforeEach(() => {
        environment.generateErrorOnNavItemsLoading = true;
      });

      afterEach(() => {
        environment.generateErrorOnNavItemsLoading = false;
      });

      it('should return rejected promise', fakeAsync(() => {
        let errorMessage: string;

        const result = childApiMethods.getNavItems() as Promise<any>;
        result.catch(e => errorMessage = e.message);

        flushMicrotasks();

        expect(errorMessage).toBeDefined();
      }));
    });

    describe('if selectedSiteId is not set', () => {
      beforeEach(() => {
        stateMock.selectedSiteId = undefined;
      });

      it('should return default nav items', async () => {
        const navItems = await childApiMethods.getNavItems() as NavItem[];

        expect(navItems.length).toBe(105);
      });
    });

    describe('if selectedSiteId is set', () => {
      beforeEach(() => {
        stateMock.selectedSiteId = {
          siteId: 1,
          accountId: 1,
        };
      });

      it('should return default nav items', async () => {
        const navItems = await childApiMethods.getNavItems() as NavItem[];

        expect(navItems.length).toBe(10);
      });
    });
  });

  describe('navigate()', () => {
    const location: NavLocation = {
      path: '/some/path',
      pathPrefix: 'some-prefix',
    };

    it('should perform navigation', () => {
      childApiMethods.navigate(location, NavigationTrigger.Menu);

      expect(stateMock.navigateCount).toBe(1);
      expect(stateMock.navigatedTo).toBe(location);
      expect(stateMock.lastNavigationTriggeredBy).toBe(NavigationTrigger.Menu);
    });

    it('should return a Promise', () => {
      const result = childApiMethods.navigate(location, NavigationTrigger.Menu) as Promise<void>;

      expect(result.then).toBeDefined();
    });

    it('should delay navigation by navigationDelay ms', fakeAsync(() => {
      let navigationResolved = false;

      const result = childApiMethods.navigate(location, NavigationTrigger.Menu) as Promise<void>;
      result.then(() => navigationResolved = true);

      tick(stateMock.navigationDelay);

      expect(navigationResolved).toBeTruthy();
    }));
  });

  describe('logout()', () => {
    it('should return a resolved promise', fakeAsync(() => {
      let resolved = false;

      const result = childApiMethods.logout() as Promise<void>;
      result.then(() => resolved = true);

      flushMicrotasks();

      expect(resolved).toBeTruthy();
    }));

    it('should return a rejected promise if state.generateAnErrorUponLogout is true', fakeAsync(() => {
      stateMock.generateAnErrorUponLogout = true;
      let rejected = false;

      const result = childApiMethods.logout() as Promise<void>;
      result.catch(() => rejected = true);

      flushMicrotasks();

      expect(rejected).toBeTruthy();
    }));
  });

  describe('onUserActivity()', () => {
    it('should increment the counter', () => {
      childApiMethods.onUserActivity();

      expect(stateMock.userActivityReported).toBe(1);
    });
  });

  describe('if it is brSM mock', () => {
    beforeEach(() => {
      const state: AppState = TestBed.get(AppState);
      (state as any).isBrSmMock = true;

      const service: ChildApiMethodsService = TestBed.get(ChildApiMethodsService);

      childApiMethods = service.getMethods();
    });

    it('should return proper methods', () => {
      expect(childApiMethods.getConfig).toBeDefined();
      expect(childApiMethods.navigate).toBeDefined();
      expect(childApiMethods.getNavItems).toBeDefined();
      expect(childApiMethods.logout).toBeDefined();
      expect(childApiMethods.onUserActivity).toBeDefined();
      expect(childApiMethods.getSites).toBeDefined();
      expect(childApiMethods.getSelectedSite).toBeDefined();
      expect(childApiMethods.updateSelectedSite).toBeDefined();
    });

    describe('getConfig()', () => {
      it('should return child config', async () => {
        const expected = {
          apiVersion: '1.2.0',
          showSiteDropdown: true,
        };

        const actual = await childApiMethods.getConfig();

        expect(actual).toEqual(expected);
      });
    });

    describe('getSites()', () => {
      it('should return sites', async () => {
        const sites = await childApiMethods.getSites() as Site[];

        expect(sites.length).toBe(8);
      });
    });

    describe('getSelectedSite()', () => {
      it('should return sites', async () => {
        const expected = {
          siteId: 10,
          accountId: 20,
        };

        stateMock.selectedSiteId = expected;

        const actual = await childApiMethods.getSelectedSite();

        expect(actual).toBe(expected);
      });
    });

    describe('updateSelectedSite()', () => {
      it('should set the selected site', () => {
        const expected = {
          siteId: 10,
          accountId: 20,
        };

        childApiMethods.updateSelectedSite(expected);

        expect(stateMock.selectedSiteId).toBe(expected);
      });
    });
  });
});
