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

import { DOCUMENT, Location } from '@angular/common';
import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ChildApi, ClientError } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Subject } from 'rxjs';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { AppSettingsMock } from '../models/dto/app-settings.mock';

import { APP_SETTINGS } from './app-settings';
import { AuthService } from './auth.service';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';
import { MainLoaderService } from './main-loader.service';
import { UserActivityService } from './user-activity.service';

describe('AuthService', () => {
  let service: AuthService;

  const errorMock$ = new Subject<ClientError>();
  const sessionExpiredMock$ = new Subject<void>();

  let connectionServiceMock: jasmine.SpyObj<ConnectionService>;
  let child1ApiMock: jasmine.SpyObj<ChildApi>;
  let child2ApiMock: jasmine.SpyObj<ChildApi>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;
  let mainLoaderServiceMock: jasmine.SpyObj<MainLoaderService>;
  let busyIndicatorServiceMock: jasmine.SpyObj<BusyIndicatorService>;
  let errorHandlingServiceMock: jasmine.SpyObj<ErrorHandlingService>;
  let locationMock: jasmine.SpyObj<Location>;
  let loggerMock: jasmine.SpyObj<NGXLogger>;
  let documentMock: jasmine.SpyObj<Document>;
  let userActivityServiceMock: jasmine.SpyObj<UserActivityService>;

  let numberOfLoginApps: number;

  beforeEach(() => {
    connectionServiceMock = jasmine.createSpyObj('MainLoaderService', {
      connect: Promise.resolve({ url: 'testUrl' }),
      disconnect: Promise.resolve(),
    });
    connectionServiceMock.onError$ = errorMock$;
    connectionServiceMock.onSessionExpired$ = sessionExpiredMock$;

    child1ApiMock = jasmine.createSpyObj('Child1Api', [
      'logout',
    ]);
    child2ApiMock = jasmine.createSpyObj('Child2Api', [
      'logout',
    ]);
    clientAppServiceMock = {} as any;
    (clientAppServiceMock as any).apps = [
      new ClientAppMock({ url: 'http://test.com', api: child1ApiMock }),
      new ClientAppMock({ url: 'http://test2.com', api: child2ApiMock }),
    ];

    mainLoaderServiceMock = jasmine.createSpyObj('MainLoaderService', [
      'show',
      'hide',
    ]);

    busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
      'setCriticalError',
    ]);

    locationMock = jasmine.createSpyObj('Location', [
      'prepareExternalUrl',
    ]);

    loggerMock = jasmine.createSpyObj('NGXLogger', [
      'error',
    ]);

    const appSettingsMock = new AppSettingsMock();
    numberOfLoginApps = appSettingsMock.loginResources.length;

    documentMock = {
      location: {
        replace: jasmine.createSpy('replace'),
      },
    } as any;

    userActivityServiceMock = jasmine.createSpyObj('UserActivityService', [
      'startPropagation',
    ]);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MainLoaderService, useValue: mainLoaderServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
        { provide: Location, useValue: locationMock },
        { provide: NGXLogger, useValue: loggerMock },
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: DOCUMENT, useValue: documentMock },
        { provide: UserActivityService, useValue: userActivityServiceMock },
      ],
    });

    service = TestBed.get(AuthService);
  });

  it('should set up listener for unauthorized errors', () => {
    spyOn(service, 'logout');

    errorMock$.next({ errorCode: 500 });
    errorMock$.next({ errorCode: 403 });

    expect(service.logout).toHaveBeenCalledTimes(1);
  });

  it('should set up listener for session expiration', () => {
    spyOn(service, 'logout');

    sessionExpiredMock$.next();

    expect(service.logout).toHaveBeenCalledWith('SessionExpired');
  });

  it('should start user activity propagation', () => {
    expect(userActivityServiceMock.startPropagation).toHaveBeenCalled();
  });

  describe('Logging in', () => {
    beforeEach(() => {
      spyOn(service, 'logout');
    });

    it('should log in all provided resources', fakeAsync(() => {
      service.loginAllResources();

      tick();

      expect(connectionServiceMock.connect).toHaveBeenCalledTimes(numberOfLoginApps);
      expect(service.logout).not.toHaveBeenCalled();
      expect(connectionServiceMock.disconnect).toHaveBeenCalledTimes(numberOfLoginApps);
    }));

    it('should logout if any of the logins fail', fakeAsync(() => {
      connectionServiceMock.connect.and.callFake(() => Promise.reject({ message: 'Error' }));

      service.loginAllResources();

      tick();

      expect(connectionServiceMock.connect).toHaveBeenCalledTimes(numberOfLoginApps);
      expect(service.logout).toHaveBeenCalledWith('SilentLoginFailed');
      expect(connectionServiceMock.disconnect).toHaveBeenCalledTimes(numberOfLoginApps);
    }));
  });

  describe('logging out', () => {
    let resolveChild1Logout: () => void;
    let rejectChild1Logout: (reason?: any) => void;
    let resolveChild2Logout: () => void;
    let rejectChild2Logout: (reason?: any) => void;

    beforeEach(() => {
      locationMock.prepareExternalUrl.and.returnValue('https://some-domain.com/base/path');
      child1ApiMock.logout.and.returnValue(new Promise((resolve, reject) => {
        resolveChild1Logout = resolve;
        rejectChild1Logout = reject;
      }));
      child2ApiMock.logout.and.returnValue(new Promise((resolve, reject) => {
        resolveChild2Logout = resolve;
        rejectChild2Logout = reject;
      }));

      service.logout('test-logout-message');
    });

    it('should tell all apps to logout', () => {
      expect(child1ApiMock.logout).toHaveBeenCalled();
      expect(child2ApiMock.logout).toHaveBeenCalled();
    });

    it('should show the main loader', () => {
      expect(mainLoaderServiceMock.show).toHaveBeenCalled();
    });

    it('should show the busy indicator', () => {
      expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
    });

    describe('if something went wrong and Child1Api.logout promise has been rejected', () => {
      beforeEach(async(() => {
        rejectChild1Logout(new Error('something went wrong'));
      }));

      it('should hide the main loader', () => {
        expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
      });

      it('should hide the busy indicator', () => {
        expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
      });

      it('should show the error to a user', () => {
        expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith('ERROR_UNABLE_TO_LOG_OUT', 'something went wrong');
      });

      it('should not redirect to the login page', () => {
        expect(documentMock.location.replace).not.toHaveBeenCalled();
      });
    });

    describe('after child application apis responded successfully', () => {
      beforeEach(async(() => {
        resolveChild1Logout();
        resolveChild2Logout();
      }));

      it('should redirect to the login location', () => {
        expect(locationMock.prepareExternalUrl).toHaveBeenCalledWith('/login/path/?loginmessage=test-logout-message');
        expect(documentMock.location.replace).toHaveBeenCalledWith('https://some-domain.com/base/path');
      });
    });
  });
});
