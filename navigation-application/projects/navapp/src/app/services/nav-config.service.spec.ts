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
import { ChildApi, NavItem, Site } from '@bloomreach/navapp-communication';
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
    new NavItemDtoMock({ id: 'internalRestItem', appIframeUrl: '/relative/url' }),
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
  let childApiMock: jasmine.SpyObj<ChildApi>;
  let loggerMock: jasmine.SpyObj<NGXLogger>;

  let rootUrl: string;

  beforeEach(() => {
    locationMock = jasmine.createSpyObj('Location', [
      'prepareExternalUrl',
    ]);
    locationMock.prepareExternalUrl.and.callFake((path: string) => Location.joinWithSlash('https://example.com', path));

    childApiMock = jasmine.createSpyObj('ChildApi', {
      getNavItems: Promise.resolve([navItemsMock[0]]),
      getSites: Promise.resolve(sitesMock),
      getSelectedSite: Promise.resolve(selectedSiteMock),
    });
    connectionServiceMock = jasmine.createSpyObj('ConnectionService', {
      connect: Promise.resolve(childApiMock),
      disconnect: Promise.resolve(),
    });

    loggerMock = jasmine.createSpyObj('NGXLogger', [
      'debug',
      'error',
    ]);

    const appSettingsMock = new AppSettingsMock({
      navConfigResources: [
        new ConfigResourceMock({
          resourceType: 'IFRAME',
          url: 'https://example.com/testIFRAMEurl',
        }) as ConfigResource,
        new ConfigResourceMock({
          resourceType: 'REST',
          url: 'https://example.com/testRESTurl',
        }) as ConfigResource,
        new ConfigResourceMock({
          resourceType: 'REST',
          url: '/internalRESTurl',
        }) as ConfigResource,
      ],
    });

    rootUrl = Location.joinWithSlash('https://example.com', appSettingsMock.basePath);

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
    connectionServiceMock = TestBed.get(ConnectionService);
    appSettings = TestBed.get(APP_SETTINGS);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('fetchNavigationConfiguration', () => {
    let configuration: Configuration;

    beforeEach(async(() => {
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
        navItems: [
          navItemsMock[0],
          { ...navItemsMock[1] },
          { ...navItemsMock[2], appIframeUrl: 'https://example.com/base/path/relative/url' },
        ],
        sites: sitesMock,
        selectedSiteId: selectedSiteMock,
      });
    });

    it('should create a connection', () => {
      expect(connectionServiceMock.connect).toHaveBeenCalledWith(appSettings.navConfigResources[0].url);
    });

    describe('logging', () => {
      it('should log starting of fetching of nav items from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching nav items from an REST endpoint 'https://example.com/testRESTurl'`);
      });

      it('should log that nav items have been successfully fetched from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items have been fetched from the REST endpoint 'https://example.com/testRESTurl'`,
          [navItemsMock[1]],
        );
      });

      it('should log normalized nav items fetched from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items fetched from the REST endpoint 'https://example.com/testRESTurl' after normalization`,
          [{ ...navItemsMock[1] }],
        );
      });

      it('should log starting of fetching of nav items from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Fetching nav items from an REST endpoint 'https://example.com/base/path/internalRESTurl'`,
        );
      });

      it('should log that nav items have been successfully fetched from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items have been fetched from the REST endpoint 'https://example.com/base/path/internalRESTurl'`,
          [navItemsMock[2]],
        );
      });

      it('should log normalized nav items fetched from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items fetched from the REST endpoint 'https://example.com/base/path/internalRESTurl' after normalization`,
          [{ ...navItemsMock[2], appIframeUrl: 'https://example.com/base/path/relative/url' }],
        );
      });

      it('should log starting of fetching of nav items from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching nav items from an iframe 'https://example.com/testIFRAMEurl'`);
      });

      it('should log that nav items have been successfully fetched an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items have been fetched from the iframe 'https://example.com/testIFRAMEurl'`,
          [navItemsMock[0]],
        );
      });

      it('should log starting of fetching of sites from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching sites from an iframe 'https://example.com/testIFRAMEurl'`);
      });

      it('should log that sites have been successfully fetched from the iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Sites have been fetched from the iframe 'https://example.com/testIFRAMEurl'`,
          sitesMock,
        );
      });

      it('should log starting of fetching of the selected site from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching a selected site from an iframe 'https://example.com/testIFRAMEurl'`);
      });

      it('should log that the selected site has been successfully fetched from the iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Selected site has been fetched from the iframe 'https://example.com/testIFRAMEurl'`,
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
          url: 'https://example.com/testRESTurl',
        }) as ConfigResource,
        new ConfigResourceMock({
          resourceType: 'REST',
          url: '/internalRESTurl',
        }) as ConfigResource,
      ];

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
        navItems: [
          { ...navItemsMock[1] },
          { ...navItemsMock[2], appIframeUrl: 'https://example.com/base/path/relative/url' },
        ],
        sites: [],
        selectedSiteId: undefined,
      });
    });
  });

  describe('fetchNavigationConfiguration if all resources failed', () => {
    let configuration: Configuration;
    let error: any;

    beforeEach(async(() => {
      connectionServiceMock.connect.and.returnValue(Promise.reject('Unable to connect to the iframe'));

      service.fetchNavigationConfiguration().then(c => configuration = c, e => error = e);

      const restReq = httpTestingController.expectOne(appSettings.navConfigResources[1].url);
      restReq.error(new ErrorEvent('Network failure'));

      const internalRestReq = httpTestingController.expectOne(`${rootUrl}${appSettings.navConfigResources[2].url}`);
      internalRestReq.error(new ErrorEvent('Network failure'));
    }));

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should return an empty configuration', () => {
      expect(configuration).toEqual({
        navItems: [],
        sites: [],
        selectedSiteId: undefined,
      });
    });

    it('should not return an error', () => {
      expect(error).toBeUndefined();
    });

    it('should log starting of fetching of nav items from the REST', () => {
      expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching nav items from an REST endpoint 'https://example.com/testRESTurl'`);
    });

    it('should log an error happened during nav items fetching from the REST', () => {
      expect(loggerMock.error).toHaveBeenCalledWith(
        `Unable to fetch nav items from the REST endpoint 'https://example.com/testRESTurl'`,
        'Http failure response for https://example.com/testRESTurl: 0 ',
      );
    });

    it('should log starting of fetching of nav items from an internal REST', () => {
      expect(loggerMock.debug).toHaveBeenCalledWith(
        `Fetching nav items from an REST endpoint 'https://example.com/base/path/internalRESTurl'`,
      );
    });

    it('should log an error happened during nav items fetching from the internal REST', () => {
      expect(loggerMock.error).toHaveBeenCalledWith(
        `Unable to fetch nav items from the REST endpoint 'https://example.com/base/path/internalRESTurl'`,
        `Http failure response for https://example.com/base/path/internalRESTurl: 0 `,
      );
    });

    it('should log starting of fetching of nav items from an iframe', () => {
      expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching nav items from an iframe 'https://example.com/testIFRAMEurl'`);
    });

    it('should log an error happened during nav items fetching from the iframe', () => {
      expect(loggerMock.error).toHaveBeenCalledWith(
        `Unable to fetch a selected site from the iframe 'https://example.com/testIFRAMEurl'`,
        'Unable to connect to the iframe',
      );
    });
  });

  describe('refetchNavItems', () => {
    let navItems: NavItem[];

    const newIframeNavItems = [
      new NavItemDtoMock({id: 'newIframeItem'}),
    ];

    beforeEach(async(() => {
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
      const expected = [
        newIframeNavItems[0],
        { ...navItemsMock[1] },
        { ...navItemsMock[2], appIframeUrl: 'https://example.com/base/path/relative/url' },
      ];

      expect(navItems).toEqual(expected);
    });

    it('should create a connection', () => {
      expect(connectionServiceMock.connect).toHaveBeenCalledWith(appSettings.navConfigResources[0].url);
    });

    describe('logging', () => {
      it('should log starting of fetching of nav items from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching nav items from an REST endpoint 'https://example.com/testRESTurl'`);
      });

      it('should log that nav items have been successfully fetched from REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items have been fetched from the REST endpoint 'https://example.com/testRESTurl'`,
          [navItemsMock[1]],
        );
      });

      it('should log starting of fetching of nav items from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Fetching nav items from an REST endpoint 'https://example.com/base/path/internalRESTurl'`,
        );
      });

      it('should log that nav items have been successfully fetched from internal REST', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items have been fetched from the REST endpoint 'https://example.com/base/path/internalRESTurl'`,
          [navItemsMock[2]],
        );
      });

      it('should log starting of fetching of nav items from an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(`Fetching nav items from an iframe 'https://example.com/testIFRAMEurl'`);
      });

      it('should log that nav items have been successfully fetched an iframe', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith(
          `Nav items have been fetched from the iframe 'https://example.com/testIFRAMEurl'`,
          [newIframeNavItems[0]],
        );
      });
    });
  });
});
