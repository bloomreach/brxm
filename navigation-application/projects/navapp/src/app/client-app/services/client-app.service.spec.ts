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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ChildApi } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { LoggerTestingModule } from 'ngx-logger/testing';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { CriticalError } from '../../error-handling/models/critical-error';
import { Connection } from '../../models/connection.model';
import { AppSettingsMock } from '../../models/dto/app-settings.mock';
import { FailedConnection } from '../../models/failed-connection.model';
import { NavItem } from '../../models/nav-item.model';
import { APP_SETTINGS } from '../../services/app-settings';
import { ClientApp } from '../models/client-app.model';

import { ClientAppService } from './client-app.service';

xdescribe('ClientAppService', () => {
  let service: ClientAppService;
  let logger: NGXLogger;

  const iframesConnectionTimeout = 200;

  const navItemsMock = [
    {
      id: 'item1',
      appIframeUrl: 'http://app1.com',
      appPath: 'some-path',
    },
    {
      id: 'item2',
      appIframeUrl: 'http://app1.com',
      appPath: 'some/path/to/another/resource',
    },
    {
      id: 'item3',
      appIframeUrl: 'http://app2.com',
      appPath: 'some-path',
    },
  ] as NavItem[];

  const appSettingsMock = new AppSettingsMock({
    iframesConnectionTimeout,
  });

  let appConnectedSpy: jasmine.Spy;
  const unsubscribe = new Subject();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        LoggerTestingModule,
      ],
      providers: [
        ClientAppService,
        { provide: APP_SETTINGS, useValue: appSettingsMock },
      ],
    });

    service = TestBed.get(ClientAppService);
    logger = TestBed.get(NGXLogger);

    appConnectedSpy = jasmine.createSpy('appConnectedSpy');
    service.appConnected$.pipe(
      takeUntil(unsubscribe),
    ).subscribe(app => {
      appConnectedSpy(app.url);
    });
  });

  afterEach(() => {
    unsubscribe.next();
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
    describe('when applications without "getConfig()" connected normally', () => {
      let initialized = false;

      beforeEach(async(() => {
        service.init(navItemsMock).then(() => initialized = true);
        service.addConnection(new Connection('http://app1.com', {}));
        service.addConnection(new Connection('http://app2.com', {}));
      }));

      it('should be completed successfully', () => {
        expect(appConnectedSpy).toHaveBeenCalledWith('http://app1.com');
        expect(appConnectedSpy).toHaveBeenCalledWith('http://app2.com');
      });

      it('should resolve the returned init promise', () => {
        expect(initialized).toBeTruthy();
      });
    });

    describe('when applications with "getConfig()" connected normally', () => {
      let initialized = false;

      beforeEach(async(() => {
        const childApi1 = jasmine.createSpyObj('ChildApi1', {
          getConfig: Promise.resolve({ apiVersion: '1.0.0' }),
        });

        const childApi2 = jasmine.createSpyObj('ChildApi2', {
          getConfig: Promise.resolve({ apiVersion: '1.0.0' }),
        });

        service.init(navItemsMock).then(() => initialized = true);

        service.addConnection(new Connection('http://app1.com', childApi1));
        service.addConnection(new Connection('http://app2.com', childApi2));
      }));

      it('should be completed successfully', () => {
        expect(appConnectedSpy).toHaveBeenCalledWith('http://app1.com');
        expect(appConnectedSpy).toHaveBeenCalledWith('http://app2.com');
      });

      it('should resolve the returned init promise', () => {
        expect(initialized).toBeTruthy();
      });
    });

    describe('when one application failed to connect', () => {
      let initialized = false;

      beforeEach(async(() => {
        const childApi = jasmine.createSpyObj('ChildApi2', {
          getConfig: Promise.resolve({ apiVersion: '1.0.0' }),
        });

        service.init(navItemsMock).then(() => initialized = true);

        service.addConnection(new FailedConnection('http://app1.com', 'some reason'));
        service.addConnection(new Connection('http://app2.com', childApi));
      }));

      it('should be completed successfully', () => {
        expect(appConnectedSpy).not.toHaveBeenCalledWith('http://app1.com');
        expect(appConnectedSpy).toHaveBeenCalledWith('http://app2.com');
      });

      it('should resolve the returned init promise', () => {
        expect(initialized).toBeTruthy();
      });
    });

    describe('when all applications failed to connect and "iframesConnectionTimeout * 1.5" ms passed', () => {
      let initialized = false;
      let rejectionReason: Error;

      beforeEach(fakeAsync(() => {
        service.init(navItemsMock).then(() => initialized = true, e => rejectionReason = e);
        service.addConnection(new FailedConnection('http://app1.com', 'some reason'));
        service.addConnection(new FailedConnection('http://app2.com', 'some reason'));

        tick(iframesConnectionTimeout * 1.5);
      }));

      it('should not be completed successfully', () => {
        expect(appConnectedSpy).not.toHaveBeenCalledWith('http://app1.com');
        expect(appConnectedSpy).not.toHaveBeenCalledWith('http://app2.com');
      });

      it('should reject the returned init promise after "iframesConnectionTimeout * 1.5" timeout', () => {
        const expectedError = new CriticalError(
          'ERROR_UNABLE_TO_CONNECT_TO_CLIENT_APP',
          'All connections to the client applications are failed',
        );

        expect(initialized).toBeFalsy();
        expect(rejectionReason).toEqual(expectedError);
      });
    });

    describe('when getConfig() is defined in ChildApi', () => {
      let childApi1: jasmine.SpyObj<ChildApi>;
      let childApi2: jasmine.SpyObj<ChildApi>;

      beforeEach(() => {
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

          service.init(navItemsMock);

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));
        }));

        it('should set the config object to a default object', () => {
          expect(service.getAppConfig('http://app1.com')).toEqual({ apiVersion: 'unknown' });
          expect(service.getAppConfig('http://app2.com')).toEqual({ apiVersion: '1.0.0' });
        });
      });

      describe('and returns the config object without apiVersion', () => {
        beforeEach(async(() => {
          childApi1.getConfig.and.returnValue(Promise.resolve({}));
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));

          service.init(navItemsMock);

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));
        }));

        it('should set the apiVersion to unknown', () => {
          expect(service.getAppConfig('http://app1.com')).toEqual({ apiVersion: 'unknown' });
          expect(service.getAppConfig('http://app2.com')).toEqual({ apiVersion: '1.0.0' });
        });
      });

      describe('and returns a rejected promise', () => {
        let initialized: boolean;

        beforeEach(async(() => {
          initialized = false;

          childApi1.getConfig.and.callFake(() => Promise.reject('some reason'));
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));

          service.init(navItemsMock).then(
            () => initialized = true,
            () => initialized = false,
          );

          service.addConnection(new Connection('http://app1.com', childApi1));
          service.addConnection(new Connection('http://app2.com', childApi2));
        }));

        it('should not reject the init promise', () => {
          expect(initialized).toBeTruthy();
          expect(service.apps.length).toBe(2);
        });

        it('should return default config for the failed app', () => {
          const expected = { apiVersion: 'unknown' };

          const actual = service.getAppConfig('http://app1.com');

          expect(actual).toEqual(expected);
        });
      });

      describe('and returns the config object with an apiVersion set', () => {
        beforeEach(async(() => {
          childApi1.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));
          childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '2.0.0' }));

          service.init(navItemsMock);

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
    let clientApiWithoutSitesSupport: ChildApi;
    let clientApiWithSitesSupport: ChildApi;

    beforeEach(async(() => {
      clientApiWithoutSitesSupport = {
        getConfig: () => Promise.resolve({ apiVersion: '1.0.0', showSiteDropdown: false }),
      };

      clientApiWithSitesSupport = {
        getConfig: () => Promise.resolve({ apiVersion: '1.0.0', showSiteDropdown: true }),
      };

      service.init(navItemsMock);

      service.addConnection(new Connection('http://app1.com', clientApiWithoutSitesSupport));
      service.addConnection(new Connection('http://app2.com', clientApiWithSitesSupport));
    }));

    it('should throw an exception if it attempts to add a connection', () => {
      const expectedError = 'An attempt to register a connection after all expected connections are registered or timeout has expired';

      const connection = new Connection('http://app1.com', {});

      expect(() => service.addConnection(connection)).toThrowError(expectedError);
    });

    it('should throw an exception if it attempts to activate an unknown app', () => {
      const expectedError = `An attempt to active unknown app 'https://unknown-app-id.com'`;

      expect(() => service.activateApplication('https://unknown-app-id.com')).toThrowError(expectedError);
    });

    it('should throw an exception if it attempts to get an unknown app', () => {
      const expectedError = `Unable to find the app 'https://unknown-app-id.com'`;

      expect(() => service.getApp('https://unknown-app-id.com')).toThrowError(expectedError);
    });

    it('should throw an exception if it attempts to get config of an unknown app', () => {
      const expectedError = `Unable to find the app 'https://unknown-app-id.com'`;

      expect(() => service.getAppConfig('https://unknown-app-id.com')).toThrowError(expectedError);
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

  describe('logging', () => {
    beforeEach(() => {
      spyOn(logger, 'debug');
    });

    describe('during initialization', () => {
      it('should log iframe urls', fakeAsync(() => {
        service.init(navItemsMock).catch(() => { });

        expect(logger.debug).toHaveBeenCalledWith('Client app iframes are expected to be loaded (2)', [
          'http://app1.com',
          'http://app2.com',
        ]);
      }));

      it('should log the added connection', () => {
        service.init(navItemsMock).catch(() => { });

        service.addConnection(new Connection('http://app1.com', {}));

        expect(logger.debug).toHaveBeenCalledWith('Connection is established to the iframe \'http://app1.com\'');
      });

      it('should log an error when a connection with unknown url is added', () => {
        spyOn(logger, 'error');

        service.init(navItemsMock).catch(() => { });

        const badConnection = new Connection('http://suspect-site.com', {});

        service.addConnection(badConnection);

        expect(logger.error).toHaveBeenCalledWith('An attempt to register a connection to an unknown url \'http://suspect-site.com\'');
      });

      describe('when getConfig() is defined in ChildApi', () => {
        let childApi1: jasmine.SpyObj<ChildApi>;
        let childApi2: jasmine.SpyObj<ChildApi>;

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

            service.init(navItemsMock);

            service.addConnection(new Connection('http://app1.com', childApi1));
            service.addConnection(new Connection('http://app2.com', childApi2));
          }));

          it('should log a warning message', () => {
            expect(logger.warn).toHaveBeenCalledWith('The app \'http://app1.com\' returned an empty config');
          });
        });

        describe('and returns the config object without apiVersion', () => {
          beforeEach(async(() => {
            childApi1.getConfig.and.returnValue(Promise.resolve({}));
            childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));

            service.init(navItemsMock);

            service.addConnection(new Connection('http://app1.com', childApi1));
            service.addConnection(new Connection('http://app2.com', childApi2));
          }));

          it('should log a warning message', () => {
            expect(logger.warn).toHaveBeenCalledWith('The app \'http://app1.com\' returned a config with an empty version');
          });
        });

        describe('and returns a rejected promise', () => {
          let initialized: boolean;

          beforeEach(async(() => {
            initialized = false;

            childApi1.getConfig.and.callFake(() => Promise.reject('some reason'));
            childApi2.getConfig.and.returnValue(Promise.resolve({ apiVersion: '1.0.0' }));

            service.init(navItemsMock).then(
              () => initialized = true,
              () => initialized = false,
            );

            service.addConnection(new Connection('http://app1.com', childApi1));
            service.addConnection(new Connection('http://app2.com', childApi2));
          }));

          it('should log a warning message', () => {
            expect(logger.warn).toHaveBeenCalledWith('Unable to load config for \'http://app1.com\'. Reason: \'some reason\'.');
          });
        });
      });
    });
  });
});
