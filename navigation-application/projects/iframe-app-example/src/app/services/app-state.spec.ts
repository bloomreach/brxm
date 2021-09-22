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
import { TestBed } from '@angular/core/testing';
import { CookieService } from 'ngx-cookie-service';

import { AppState } from './app-state';

describe('AppState', () => {
  let state: AppState;

  let locationMock: jasmine.SpyObj<Location>;
  let cookiesServiceMock: jasmine.SpyObj<CookieService>;

  beforeEach(() => {
    locationMock = jasmine.createSpyObj('Location', [
      'path',
    ]);
    cookiesServiceMock = jasmine.createSpyObj('Location', [
      'get',
      'set',
    ]);

    TestBed.configureTestingModule({
      providers: [
        AppState,
        { provide: Location, useValue: locationMock },
        { provide: CookieService, useValue: cookiesServiceMock },
      ],
    });

    state = TestBed.inject(AppState);
  });

  it('should return default state', () => {
    expect(state.navappCommunicationImplementationApiVersion).toBe('1.0.0');
    expect(state.navigateCount).toBe(0);
    expect(state.navigatedTo).toBe(undefined);
    expect(state.buttonClickedCounter).toBe(0);
    expect(state.overlaid).toBe(false);
    expect(state.userActivityReported).toBe(0);
    expect(state.historyPushStateCount).toBe(0);
    expect(state.historyReplaceStateCount).toBe(0);
    expect(state.generateAnErrorUponLogout).toBe(false);
  });

  describe('isBrSmMock', () => {
    it('should return false', () => {
      locationMock.path.and.returnValue('/some/path');

      const actual = state.isBrSmMock;

      expect(actual).toBeFalsy();
    });

    it('should return true', () => {
      locationMock.path.and.returnValue('/brsm/some/path');

      const actual = state.isBrSmMock;

      expect(actual).toBeTruthy();
    });
  });

  describe('selectedSiteId', () => {
    it('should be set', () => {
      state.selectedSiteId = {
        siteId: 1,
        accountId: 2,
      };

      expect(cookiesServiceMock.set).toHaveBeenCalledWith('EXAMPLE_APP_SITE_ID', '2,1');
    });

    it('should be retrievable', () => {
      cookiesServiceMock.get.and.returnValue('2,3');

      const actual = state.selectedSiteId;

      expect(actual).toEqual({
        siteId: 3,
        accountId: 2,
      });
    });

    it('should be the first available site if it is impossible to retrieve it from the cookie', () => {
      cookiesServiceMock.get.and.returnValue('');

      const actual = state.selectedSiteId;

      expect(actual).toEqual(jasmine.objectContaining({
        siteId: -1,
        accountId: 1,
      }));
    });
  });
});
