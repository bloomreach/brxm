/*!
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

import { DOCUMENT, Location } from '@angular/common';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ChildPromisedApi, ClientError } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Subject } from 'rxjs';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { AppSettingsMock } from '../models/dto/app-settings.mock';

import { APP_SETTINGS } from './app-settings';
import { AuthService } from './auth.service';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';

describe('AuthService', () => {
  let service: AuthService;
  let childApiMock: jasmine.SpyObj<ChildPromisedApi>;

  const errorMock$ = new Subject<ClientError>();
  const sessionExpiredMock$ = new Subject();

  const connectionServiceMock = {
    onError$: errorMock$,
    onSessionExpired$: sessionExpiredMock$,
    createConnection: undefined,
    removeConnection: undefined,
  };

  const clientAppServiceMock = {
    apps: undefined,
  };

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const appSettingsMock = new AppSettingsMock();
  const numberOfLoginApps = appSettingsMock.loginResources.length;
  const numberOfLogoutApps = appSettingsMock.logoutResources.length;

  const documentMock = {
    location: {
      replace: undefined,
    },
  };

  const locationMock = jasmine.createSpyObj('Location', [
    'prepareExternalUrl',
  ]);

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'error',
  ]);

  beforeEach(() => {
    connectionServiceMock.createConnection = jasmine
      .createSpy('createConnection')
      .and
      .returnValue(Promise.resolve({ url: 'testUrl' }));
    connectionServiceMock.removeConnection = jasmine
      .createSpy('removeConnection')
      .and
      .returnValue(Promise.resolve());

    childApiMock = jasmine.createSpyObj('ChildApi', [
      'logout',
    ]);
    clientAppServiceMock.apps = [
      new ClientAppMock({ url: 'http://test.com', api: childApiMock }),
      new ClientAppMock({ url: 'http://test2.com', api: childApiMock }),
    ];

    documentMock.location.replace = jasmine.createSpy('replace');

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: Location, useValue: locationMock },
        { provide: NGXLogger, useValue: loggerMock },
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: DOCUMENT, useValue: documentMock },
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

  describe('Logging in', () => {
    beforeEach(() => {
      spyOn(service, 'logout');
    });

    it('should log in all provided resources', fakeAsync(() => {
      service.loginAllResources();

      tick();

      expect(connectionServiceMock.createConnection).toHaveBeenCalledTimes(numberOfLoginApps);
      expect(service.logout).not.toHaveBeenCalled();
      expect(connectionServiceMock.removeConnection).toHaveBeenCalledTimes(numberOfLoginApps);
    }));

    it('should logout if any of the logins fail', fakeAsync(() => {
      connectionServiceMock.createConnection.and.callFake(() => Promise.reject({ message: 'Error' }));

      service.loginAllResources();

      tick();

      expect(connectionServiceMock.createConnection).toHaveBeenCalledTimes(numberOfLoginApps);
      expect(service.logout).toHaveBeenCalledWith('SilentLoginFailed');
      expect(connectionServiceMock.removeConnection).toHaveBeenCalledTimes(numberOfLoginApps);
    }));
  });

  describe('logging out', () => {
    it('should tell all apps to logout', fakeAsync(() => {
      service.logout('Test logout message');
      tick(1000);

      expect(childApiMock.logout).toHaveBeenCalledTimes(numberOfLogoutApps);
    }));

    it('should redirect to the login location', fakeAsync(() => {
      locationMock.prepareExternalUrl.and.returnValue('https://some-domain.com/base/path');
      const logoutMessage = 'test-logout-message';

      service.logout(logoutMessage);

      tick(1000);

      expect(locationMock.prepareExternalUrl).toHaveBeenCalledWith('/login/path/?loginmessage=test-logout-message');
      expect(documentMock.location.replace).toHaveBeenCalledWith('https://some-domain.com/base/path');
    }));
  });
});
