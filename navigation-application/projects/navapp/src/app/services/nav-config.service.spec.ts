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

import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import * as navappCommunication from '@bloomreach/navapp-communication';

import { GlobalSettingsMock } from '../models/dto/global-settings.mock';
import { NavItemMock } from '../models/dto/nav-item.mock';

import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';

describe('NavConfigService', () => {
  let http: HttpClient;
  let httpTestingController: HttpTestingController;
  let navConfigService: NavConfigService;
  let globalSettingsService: GlobalSettingsService;

  const navConfig = [
    new NavItemMock({ id: 'testId1' }),
    new NavItemMock({ id: 'testId2' }),
  ];

  const mockSites: navappCommunication.Site[] = [
    {
      siteId: -1,
      accountId: 1,
      name: 'www.company.com',
      subGroups: [],
    },
  ];

  const globalSettingsServiceMock = new GlobalSettingsMock();

  const selectedSite = { accountId: 1, siteId: 2 };

  const childApiMock = jasmine.createSpyObj('childApi', {
    getNavItems: Promise.resolve(navConfig),
    getSites: Promise.resolve(mockSites),
    getSelectedSite: Promise.resolve(selectedSite),
  });

  const connectionMock = jasmine
    .createSpy('connectToChild')
    .and.returnValue(Promise.resolve(childApiMock));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: GlobalSettingsService, useValue: globalSettingsServiceMock },
        NavConfigService,
      ],
    });

    http = TestBed.get(HttpClient);
    httpTestingController = TestBed.get(HttpTestingController);
    navConfigService = TestBed.get(NavConfigService);
    globalSettingsService = TestBed.get(GlobalSettingsService);

    spyOnProperty(navappCommunication, 'connectToChild', 'get').and.returnValue(
      connectionMock,
    );
  });

  describe('initialization', () => {
    it('should fetch resources', fakeAsync(() => {
      const RESTNavItem = new NavItemMock({
        id: 'testItem',
        appIframeUrl: 'testurl',
        appPath: 'test path',
      });

      const totalNavItems = navConfig.concat(RESTNavItem);

      navConfigService.init();
      tick();

      const req = httpTestingController.expectOne('testRESTurl');
      req.flush([RESTNavItem]);

      navConfigService.navItems$.subscribe(items => {
        expect(items).toEqual(totalNavItems);
      });
      navConfigService.sites$.subscribe(items => {
        expect(items).toEqual(mockSites);
      });
      navConfigService.selectedSite$.subscribe(site => {
        expect(site.accountId).toEqual(1);
        expect(site.siteId).toEqual(2);
      });

      httpTestingController.verify();
    }));
  });
});
