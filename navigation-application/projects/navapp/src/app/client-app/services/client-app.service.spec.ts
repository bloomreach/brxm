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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ChildPromisedApi } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { LoggerTestingModule } from 'ngx-logger/testing';

import { Connection } from '../../models/connection.model';
import { AppSettingsMock } from '../../models/dto/app-settings.mock';
import { FailedConnection } from '../../models/failed-connection.model';
import { APP_SETTINGS } from '../../services/app-settings';
import { NavItemService } from '../../services/nav-item.service';
import { ClientApp } from '../models/client-app.model';

import { ClientAppService } from './client-app.service';

describe('ClientAppService', () => {
  let service: ClientAppService;
  let logger: NGXLogger;

  const iframesConnectionTimeout = 200;

  const navItemsMock = [
    {
      id: 'item1',
      appIframeUrl: 'http://app1.com',
      path: 'some-path',
    },
    {
      id: 'item2',
      appIframeUrl: 'http://app1.com',
      path: 'some/path/to/another/resource',
    },
    {
      id: 'item3',
      appIframeUrl: 'http://app2.com',
      path: 'some-path',
    },
  ];

  const navItemServiceMock = {
    navItems: navItemsMock,
  } as any;

  const appSettingsMock = new AppSettingsMock({
    iframesConnectionTimeout,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        LoggerTestingModule,
      ],
      providers: [
        ClientAppService,
        { provide: NavItemService, useValue: navItemServiceMock },
        { provide: APP_SETTINGS, useValue: appSettingsMock },
      ],
    });

    service = TestBed.get(ClientAppService);
    logger = TestBed.get(NGXLogger);

    spyOn(logger, 'debug');
  });

  it('should exist', () => {
    expect(service).toBeDefined();
  });

  describe('before initialized', () => {
    it('should emit an empty array of urls', () => {
      const expected: string[] = [];

      let actual: string[];

      service.urls$.subscribe(urls => actual = urls);

      expect(actual).toEqual(expected);
    });

    it('doesActiveAppSupportSites should return false', () => {
      const actual = service.doesActiveAppSupportSites;

      expect(actual).toBeFalsy();
    });
  });

  describe('initialization', () => {
    it('should log iframe urls', () => {
      service.init().catch(() => {});

      expect(logger.debug).toHaveBeenCalledWith('Client app iframes are expected to be loaded', [
        'http://app1.com',
        'http://app2.com',
      ]);
    });

    it('should be completed successfully', fakeAsync(() => {
      const expected = [
        new ClientApp('http://app1.com', {}),
        new ClientApp('http://app2.com', {}),
      ];
      let actual: ClientApp[];

      service.init().then(() => actual = service.apps);

      service.addConnection(new Connection('http://app1.com', {}));
      service.addConnection(new Connection('http://app2.com', {}));

      tick();

      expect(actual).toEqual(expected);
    }));

    it('should log the added connection', () => {
      service.init().catch(() => {});

      service.addConnection(new Connection('http://app1.com', {}));

      expect(logger.debug).toHaveBeenCalledWith('Connection is established to the iframe \'http://app1.com\'');
    });

    it('should be finished when the timeout is reached but not all connections are registered', fakeAsync(() => {
      let initialized = false;

      service.init().then(() => initialized = true);

      tick(iframesConnectionTimeout / 2);

      service.addConnection(new Connection('http://app1.com', {}));

      tick(iframesConnectionTimeout);

      expect(initialized).toBeTruthy();
      expect(service.apps.length).toBe(1);
    }));

    it('should trigger an error error when a connection with unknown url is added', () => {
      spyOn(logger, 'error');

      service.init().catch(() => {});

      const badConnection = new Connection('http://suspect-site.com', {});

      service.addConnection(badConnection);

      expect(logger.error).toHaveBeenCalledWith('An attempt to register the connection to an unknown url \'http://suspect-site.com\'');
    });

    it('should reject the promise when all connections are failed', fakeAsync(() => {
      let initialized: boolean;

      service.init().catch(() => initialized = false);

      const badConnection = new Connection('http://suspect-site.com', {});
      service.addConnection(badConnection);

      const failedConnection = new FailedConnection('http://app1.com', 'some reason');
      service.addConnection(failedConnection);

      tick();

      expect(initialized).toBe(false);
      expect(service.apps.length).toBe(0);
    }));

    describe('when getConfig() is defined in ChildApi', () => {
      let childApi1: jasmine.SpyObj<ChildPromisedApi>;
      let childApi2: jasmine.SpyObj<ChildPromisedApi>;

      beforeEach(() => {
        spyOn(logger, 'warn');

        childApi1 = jasmine.createSpyObj('ChildApi1', [
          'getConfig',
        ]);

        childApi2 = jasmine.createSpyObj('ChildApi2', [
          'getConfig',
        ]);
      });

      describe('and returns undefined', () => {
        beforeEach(async(() => {
          childApi1.getConfig.and.returnValue(Promise.resolve(undefined));
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));

          service.init();

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));
        }));

        it('should set the config object to a default object', () => {
          expect(service.getAppConfig('http://app1.com')).toEqual({ apiVersion: 'unknown' });
          expect(service.getAppConfig('http://app2.com')).toEqual({ apiVersion: '1.0.0' });
        });

        it('should provide a warning message', () => {
          expect(logger.warn).toHaveBeenCalledWith('The app \'http://app1.com\' returned an empty config');
        });
      });

      describe('and returns the config object without apiVersion', () => {
        beforeEach(async(() => {
          childApi1.getConfig.and.returnValue(Promise.resolve({}));
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));

          service.init();

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));
        }));

        it('should set the apiVersion to unknown', () => {
          expect(service.getAppConfig('http://app1.com')).toEqual({ apiVersion: 'unknown' });
          expect(service.getAppConfig('http://app2.com')).toEqual({ apiVersion: '1.0.0' });
        });

        it('should provide a warning message', () => {
          expect(logger.warn).toHaveBeenCalledWith('The app \'http://app1.com\' returned a config with an empty version');
        });
      });

      describe('and return a rejected promise', () => {
        beforeEach(() => {
          childApi1.getConfig.and.callFake(() => Promise.reject());
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));
        });

        it('should reject the init promise', fakeAsync(() => {
          let initialized: boolean;

          service.init().catch(() => initialized = false);

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));

          tick();

          expect(initialized).toBe(false);
          expect(service.apps.length).toBe(0);
        }));
      });

      describe('and returns the config object with an apiVersion set', () => {
        beforeEach(async(() => {
          childApi1.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '2.0.0' }));

          service.init();

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));
        }));

        it('should be completed successfully', () => {
          expect(service.getAppConfig('http://app1.com')).toEqual({ apiVersion: '1.0.0' });
          expect(service.getAppConfig('http://app2.com')).toEqual({ apiVersion: '2.0.0' });
        });
      });
    });
  });

  describe('when initialized', () => {
    let clientApiWithoutSitesSupport: ChildPromisedApi;
    let clientApiWithSitesSupport: ChildPromisedApi;

    beforeEach(async(() => {
      clientApiWithoutSitesSupport = {
        getConfig: () => Promise.resolve({ apiVersion: '1.0.0', showSiteDropdown: false }),
      };

      clientApiWithSitesSupport = {
        getConfig: () => Promise.resolve({ apiVersion: '1.0.0', showSiteDropdown: true }),
      };

      service.init();

      service.addConnection(new Connection('http://app1.com', clientApiWithoutSitesSupport));
      service.addConnection(new Connection('http://app2.com', clientApiWithSitesSupport));
    }));

    it('should throw an exception if it attempts to activate an unknown app', () => {
      expect(() => service.activateApplication('https://unknown-app-id.com')).toThrow();
    });

    it('should throw an exception if it attempts to get an unknown app', () => {
      expect(() => service.getApp('https://unknown-app-id.com')).toThrow();
    });

    it('should throw an exception if it attempts to get config of an unknown app', () => {
      expect(() => service.getAppConfig('https://unknown-app-id.com')).toThrow();
    });

    it('should return a list of connected apps', () => {
      const expected = [
        new ClientApp('http://app1.com', clientApiWithoutSitesSupport),
        new ClientApp('http://app2.com', clientApiWithSitesSupport),
      ];

      const actual = service.apps;

      expect(actual.length).toBe(2);
      expect(expected).toEqual(actual);
    });

    it('should return an app by id', () => {
      const expected = new ClientApp('http://app1.com', clientApiWithoutSitesSupport);

      const actual = service.getApp('http://app1.com');

      expect(actual).toEqual(expected);
    });

    it('should return undefined before some application has been activated', () => {
      expect(service.activeApp).toBeUndefined();
    });

    it('should return the active application', () => {
      const expected = new ClientApp('http://app1.com', clientApiWithoutSitesSupport);

      service.activateApplication('http://app1.com');

      const actual = service.activeApp;

      expect(actual).toEqual(expected);
    });

    describe('when app is active', () => {
      beforeEach(() => {
        service.activateApplication('http://app2.com');
      });

      it('should check whether the application supports sites with success', () => {
        const expected = true;

        const actual = service.doesActiveAppSupportSites;

        expect(expected).toBe(actual);
      });

      it('should check whether the application supports sites without success', () => {
        service.activateApplication('http://app1.com');

        const expected = false;

        const actual = service.doesActiveAppSupportSites;

        expect(expected).toBe(actual);
      });
    });
  });
});
