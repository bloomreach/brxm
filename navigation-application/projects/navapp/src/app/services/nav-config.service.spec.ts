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

import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { async, TestBed } from '@angular/core/testing';
import { ChildPromisedApi, NavItem, Site } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { AppSettings } from '../models/dto/app-settings.dto';
import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { ConfigResource } from '../models/dto/config-resource.dto';
import { ConfigResourceMock } from '../models/dto/config-resource.mock';
import { NavItemDtoMock } from '../models/dto/nav-item-dto.mock';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';
import { Configuration, NavConfigService } from './nav-config.service';

describe('NavConfigService', () => {
  let service: NavConfigService;
  let http: HttpClient;
  let httpTestingController: HttpTestingController;
  let appSettings: AppSettings;

  const navItemsMock = [
    new NavItemDtoMock({ id: 'iframeItem' }),
    new NavItemDtoMock({ id: 'restItem' }),
    new NavItemDtoMock({ id: 'internalRestItem' }),
  ];

  const sitesMock: Site[] = [
    {
      siteId: 1,
      accountId: 123,
      isNavappEnabled: true,
      name: 'test1',
    },
    {
      siteId: 2,
      accountId: 123,
      isNavappEnabled: true,
      name: 'test2',
    },
  ];

  const selectedSiteMock = {
    siteId: sitesMock[0].siteId,
    accountId: sitesMock[0].accountId,
  };

  let locationMock: jasmine.SpyObj<Location>;
  let connectionServiceMock: jasmine.SpyObj<ConnectionService>;
  let childApiMock: jasmine.SpyObj<ChildPromisedApi>;
  let loggerMock: jasmine.SpyObj<NGXLogger>;

  beforeEach(() => {
    locationMock = jasmine.createSpyObj('Location', [
      'prepareExternalUrl',
    ]);
    locationMock.prepareExternalUrl.and.callFake((path: string) => path);

    childApiMock = jasmine.createSpyObj('ChildPromisedApi', {
      getNavItems: [navItemsMock[0]],
      getSites: sitesMock,
      getSelectedSite: selectedSiteMock,
    });
    connectionServiceMock = jasmine.createSpyObj('ConnectionService', {
      connect: Promise.resolve(childApiMock),
      disconnect: Promise.resolve(),
    });

    loggerMock = jasmine.createSpyObj('NGXLogger', [
      'debug',
    ]);

    const appSettingsMock = new AppSettingsMock();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NavConfigService,
        { provide: Location, useValue: locationMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: NGXLogger, useValue: loggerMock },
        { provide: APP_SETTINGS, useValue: appSettingsMock },
      ],
    });

    service = TestBed.get(NavConfigService);
    http = TestBed.get(HttpClient);
    httpTestingController = TestBed.get(HttpTestingController);
    appSettings = TestBed.get(APP_SETTINGS);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('fetchNavigationConfiguration', () => {
    let configuration: Configuration;

    beforeEach(async(() => {
      const rootUrl = appSettings.basePath;

      service.fetchNavigationConfiguration().then(x => configuration = x);

      const restReq = httpTestingController.expectOne(appSettings.navConfigResources[1].url);
      restReq.flush([navItemsMock[1]]);

      const internalRestReq = httpTestingController.expectOne(`${rootUrl}${appSettings.navConfigResources[2].url}`);
      internalRestReq.flush([navItemsMock[2]]);
    }));

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should fetch the configuration', () => {
      expect(configuration).toEqual({
        navItems: navItemsMock,
        sites: sitesMock,
        selectedSiteId: selectedSiteMock,
      });
    });

    it('should create a connection', () => {
      expect(connectionServiceMock.connect).toHaveBeenCalledWith(appSettings.navConfigResources[0].url);
    });

    describe('logging', () => {
      it('should log starting of fetching of nav items from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching nav items from an REST endpoint \'\/testRESTurl\'');
      });

      it('should log that nav items have been successfully fetched from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Nav items have been fetched from the REST endpoint \'\/testRESTurl\'',
          [navItemsMock[1]],
        );
      });

      it('should log starting of fetching of nav items from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching nav items from an Internal REST endpoint \'\/internalRESTurl\'');
      });

      it('should log that nav items have been successfully fetched from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Nav items have been fetched from the REST endpoint \'\/base\/path\/internalRESTurl\'',
          [navItemsMock[2]],
        );
      });

      it('should log starting of fetching of nav items from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching nav items from an iframe \'\/testIFRAMEurl\'');
      });

      it('should log that nav items have been successfully fetched an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Nav items have been fetched from the iframe \'\/testIFRAMEurl\'',
          [navItemsMock[0]],
        );
      });

      it('should log starting of fetching of sites from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching sites from an iframe \'\/testIFRAMEurl\'');
      });

      it('should log that sites have been successfully fetched an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Sites have been fetched from the iframe \'/testIFRAMEurl\'',
          sitesMock,
        );
      });

      it('should log starting of fetching of the selected site from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching a selected site from an iframe \'\/testIFRAMEurl\'');
      });

      it('should log that the selected site has been successfully fetched an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Selected site has been fetched from the iframe \'/testIFRAMEurl\'',
          selectedSiteMock,
        );
      });
    });
  });

  describe('fetchNavigationConfiguration for only REST config resources', () => {
    let configuration: Configuration;

    beforeEach(async(() => {
      appSettings.navConfigResources = [
        new ConfigResourceMock({
          resourceType: 'REST',
          url: '/testRESTurl',
        }) as ConfigResource,
        new ConfigResourceMock({
          resourceType: 'INTERNAL_REST',
          url: '/internalRESTurl',
        }) as ConfigResource,
      ];

      const rootUrl = appSettings.basePath;

      service.fetchNavigationConfiguration().then(x => configuration = x);

      const restReq = httpTestingController.expectOne(appSettings.navConfigResources[0].url);
      restReq.flush([navItemsMock[1]]);

      const internalRestReq = httpTestingController.expectOne(`${rootUrl}${appSettings.navConfigResources[1].url}`);

      internalRestReq.flush([navItemsMock[2]]);
    }));

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should fetch the configuration', () => {
      expect(configuration).toEqual({
        navItems: navItemsMock.slice(1, 3),
        sites: [],
        selectedSiteId: undefined,
      });
    });
  });

  describe('refetchNavItems', () => {
    let navItems: NavItem[];

    const newIframeNavItems = [
      new NavItemDtoMock({id: 'newIframeItem'}),
    ];

    beforeEach(async(() => {
      const rootUrl = appSettings.basePath;

      childApiMock.getNavItems.and.returnValue(newIframeNavItems);

      service.refetchNavItems().then(x => navItems = x);

      const restReq = httpTestingController.expectOne(appSettings.navConfigResources[1].url);
      restReq.flush([navItemsMock[1]]);

      const internalRestReq = httpTestingController.expectOne(`${rootUrl}${appSettings.navConfigResources[2].url}`);

      internalRestReq.flush([navItemsMock[2]]);
    }));

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should refetch the nav items', () => {
      const expected = newIframeNavItems.concat(navItemsMock.slice(1));

      expect(navItems).toEqual(expected);
    });

    it('should create a connection', () => {
      expect(connectionServiceMock.connect).toHaveBeenCalledWith(appSettings.navConfigResources[0].url);
    });

    describe('logging', () => {
      it('should log starting of fetching of nav items from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching nav items from an REST endpoint \'\/testRESTurl\'');
      });

      it('should log that nav items have been successfully fetched from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Nav items have been fetched from the REST endpoint \'\/testRESTurl\'',
          [navItemsMock[1]],
        );
      });

      it('should log starting of fetching of nav items from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching nav items from an Internal REST endpoint \'\/internalRESTurl\'');
      });

      it('should log that nav items have been successfully fetched from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Nav items have been fetched from the REST endpoint \'\/base\/path\/internalRESTurl\'',
          [navItemsMock[2]],
        );
      });

      it('should log starting of fetching of nav items from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('Fetching nav items from an iframe \'\/testIFRAMEurl\'');
      });

      it('should log that nav items have been successfully fetched an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          'Nav items have been fetched from the iframe \'\/testIFRAMEurl\'',
          [newIframeNavItems[0]],
        );
      });
    });
  });
});
