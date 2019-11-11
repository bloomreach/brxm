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

import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Site } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { AppSettings } from '../models/dto/app-settings.dto';
import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { ConfigResource } from '../models/dto/config-resource.dto';
import { ConfigResourceMock } from '../models/dto/config-resource.mock';
import { NavItemMock } from '../models/dto/nav-item.mock';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';
import { NavConfigService } from './nav-config.service';
import { NavItemService } from './nav-item.service';
import { SiteService } from './site.service';

describe('NavConfigService', () => {
  let http: HttpClient;
  let httpTestingController: HttpTestingController;
  let navConfigService: Partial<jasmine.SpyObj<NavConfigService>>;
  let navItemService: NavItemService;
  let siteService: SiteService;
  let appSettings: AppSettings;

  const navItems = [
    new NavItemMock({ id: 'iframeItem' }),
    new NavItemMock({ id: 'restItem' }),
    new NavItemMock({ id: 'internalRestItem' }),
  ];

  const locationMock = jasmine.createSpyObj('Location', [
    'prepareExternalUrl',
  ]);
  locationMock.prepareExternalUrl.and.callFake((path: string) => path);

  const sites: Site[] = [
    {
      siteId: 1,
      accountId: 123,
      name: 'test1',
    },
    {
      siteId: 2,
      accountId: 123,
      name: 'test2',
    },
  ];

  const selectedSite = {
    siteId: sites[0].siteId,
    accountId: sites[0].accountId,
  };

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

  beforeEach(() => {
    const appSettingsMock = new AppSettingsMock();

    const siteServiceMock = {
      sites: [],
      setSelectedSite: jasmine.createSpy('setSelectedSite'),
    };

    const navItemServiceMock = {
      navItems: [],
    };

    const connectionServiceMock = jasmine.createSpyObj('ConnectionService', {
      createConnection: Promise.resolve({
        url: 'testIFRAMEurl',
        api: {
          getNavItems: () => [navItems[0]],
          getSites: () => sites,
          getSelectedSite: () => selectedSite,
        },
      }),
      removeConnection: Promise.resolve(),
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NavConfigService,
        { provide: Location, useValue: locationMock },
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: NavItemService, useValue: navItemServiceMock },
        { provide: SiteService, useValue: siteServiceMock },
        { provide: NGXLogger, useValue: loggerMock },
      ],
    });

    http = TestBed.get(HttpClient);
    httpTestingController = TestBed.get(HttpTestingController);
    navConfigService = TestBed.get(NavConfigService);
    siteService = TestBed.get(SiteService);
    navItemService = TestBed.get(NavItemService);
    appSettings = TestBed.get(APP_SETTINGS);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('after initialization', () => {
    it('should fetch resources', fakeAsync(() => {
      const rootUrl = appSettings.basePath;

      navConfigService.init();

      const restReq = httpTestingController.expectOne(appSettings.navConfigResources[1].url);
      restReq.flush([navItems[1]]);

      const internalRestReq = httpTestingController.expectOne(`${rootUrl}${appSettings.navConfigResources[2].url}`);
      internalRestReq.flush([navItems[2]]);

      tick();

      expect(navItemService.navItems).toEqual(navItems);
      expect(siteService.sites).toEqual(sites);
      expect(siteService.setSelectedSite).toHaveBeenCalledWith(selectedSite);
    }));
  });
});
