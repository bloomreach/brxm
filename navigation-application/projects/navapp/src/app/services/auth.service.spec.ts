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

import { DOCUMENT } from '@angular/common';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ClientError } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { AppSettings } from '../models/dto/app-settings.dto';
import { AppSettingsMock } from '../models/dto/app-settings.mock';

import { APP_SETTINGS } from './app-settings';
import { AuthService } from './auth.service';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';

describe('AuthService', () => {
  let authService: AuthService;
  let appSettings: jasmine.SpyObj<AppSettings>;
  let connectionService: jasmine.SpyObj<ConnectionService>;
  let clientAppService: jasmine.SpyObj<ClientAppService>;

  const mockErrorStream = new Subject<ClientError>();
  const mockLogoutApi = {
    logout: jasmine.createSpy('logout'),
  };

  const documentMock = {
    location: {
      replace: jasmine.createSpy('replace'),
    },
  };

  beforeEach(() => {
    const appSettingsMock = new AppSettingsMock();

    const connectionServiceMock = {
      onError$: mockErrorStream,
      createConnection: jasmine
        .createSpy('createConnection')
        .and
        .returnValue(Promise.resolve({ url: 'testUrl' })),
      removeConnection: jasmine
        .createSpy('removeConnection')
        .and
        .returnValue(Promise.resolve()),
    };

    const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    const clientAppServiceMock = {
      apps: [
        new ClientAppMock({ url: 'http://test.com', api: mockLogoutApi }),
        new ClientAppMock({ url: 'http://test2.com', api: mockLogoutApi }),
      ],
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: DOCUMENT, useValue: documentMock },
      ],
    });

    authService = TestBed.get(AuthService);
    appSettings = TestBed.get(APP_SETTINGS);
    connectionService = TestBed.get(ConnectionService);
    clientAppService = TestBed.get(ClientAppService);
  });

  it('should set up listener for unauthorized errors', () => {
    spyOn(authService, 'logout');

    mockErrorStream.next({ errorCode: 500 });
    mockErrorStream.next({ errorCode: 403 });

    expect(authService.logout).toHaveBeenCalledTimes(1);
  });

  describe('Logging in', () => {
    beforeEach(() => {
      spyOn(authService, 'logout');
    });

    it('should login all provided resources', fakeAsync(() => {
      const numberOfLoginApps = appSettings.loginResources.length;

      authService.loginAllResources();

      tick();

      expect(connectionService.createConnection).toHaveBeenCalledTimes(numberOfLoginApps);
      expect(authService.logout).not.toHaveBeenCalled();
      expect(connectionService.removeConnection).toHaveBeenCalledTimes(numberOfLoginApps);
    }));

    it('should logout if any of the logins fail', fakeAsync(() => {
      connectionService.createConnection.and.returnValue(Promise.reject({ message: 'Error' }));
      const numberOfLoginApps = appSettings.loginResources.length;

      authService.loginAllResources();

      tick();

      expect(connectionService.createConnection).toHaveBeenCalledTimes(numberOfLoginApps);
      expect(authService.logout).toHaveBeenCalled();
      expect(connectionService.removeConnection).toHaveBeenCalledTimes(numberOfLoginApps);
    }));
  });

  describe('logging out', () => {
    it('should tell all apps to logout', fakeAsync(() => {
      const numberOfLogoutApps = appSettings.logoutResources.length;

      authService.logout('Test logout message');
      tick();

      expect(mockLogoutApi.logout).toHaveBeenCalledTimes(numberOfLogoutApps);
    }));

    it('should redirect to the login location', fakeAsync(() => {
      const logoutMessage = 'test logout message';
      authService.logout(logoutMessage);
      tick();

      const loginLocation: string = documentMock.location.replace.calls.mostRecent().args[0];

      expect(documentMock.location.replace).toHaveBeenCalled();
      expect(loginLocation.includes(logoutMessage)).toBe(true);
    }));
  });
});
