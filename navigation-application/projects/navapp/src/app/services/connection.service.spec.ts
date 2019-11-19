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

import { RendererFactory2 } from '@angular/core';
import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import * as commLib from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { UserSettingsMock } from '../models/dto/user-settings.mock';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';
import { USER_SETTINGS } from './user-settings';

describe('ConnectionService', () => {
  let service: ConnectionService;
  let logger: NGXLogger;

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

  let connectToChildSpy: jasmine.Spy;
  let parentMethods: commLib.ParentApi;
  const childApiMock: commLib.ChildPromisedApi = {};

  beforeEach(() => {
    const appSettingsMock = new AppSettingsMock();
    const userSettingsMock = new UserSettingsMock();
    const rendererMock = {
      createRenderer: () => ({
        appendChild: () => { },
        removeChild: () => { },
      }),
    };

    connectToChildSpy = spyOnProperty(commLib, 'connectToChild');
    connectToChildSpy.and.returnValue(parentConfig => {
      parentMethods = parentConfig.methods;

      return Promise.resolve({});
    });

    TestBed.configureTestingModule({
      providers: [
        ConnectionService,
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: USER_SETTINGS, useValue: userSettingsMock },
        { provide: RendererFactory2, useValue: rendererMock },
        { provide: NGXLogger, useValue: loggerMock },
      ],
    });

    service = TestBed.get(ConnectionService);
    logger = TestBed.get(NGXLogger);
  });

  it('should create a connection', fakeAsync(() => {
    const url = 'testUrl';

    service
      .createConnection(url)
      .then(connection => {
        expect(connection.url.includes(url)).toBe(true);
      });

    tick();
  }));

  it('should remove a connection', fakeAsync(() => {
    const url = 'testUrl';
    service
      .createConnection(url)
      .then(connection => {
        service.removeConnection(connection.url);
        expect(service.getConnection(connection.url)).toBeUndefined();
      });

    tick();
  }));

  it('should connect to an iframe', async(() => {
    const iframeMock = {
      src: 'https://app.com',
    } as any;

    service.connectToIframe(iframeMock).then(connection => {
      expect(connection).toBeDefined();
      expect(connection.url).toEqual('https://app.com');
      expect(connection.iframe).toEqual(iframeMock);
      expect(connection.api).toEqual(childApiMock);
    });
  }));

  it('should reject the promise when a connection can not be established', fakeAsync(() => {
    connectToChildSpy.and.returnValue(() => Promise.reject('some error'));

    const iframeMock = {
      src: 'https://app.com',
    } as any;

    let catchedError: Error;

    service.connectToIframe(iframeMock).catch(error => catchedError = error);

    tick();

    expect(catchedError).toBeDefined();
    expect(catchedError.message).toBe('Could not create connection for \'https://app.com\': some error');
  }));

  describe('when the iframe is connected', () => {
    beforeEach(async(() => {
      const iframeMock = {
        src: 'https://some-app.com',
      } as any;

      service.connectToIframe(iframeMock);
    }));

    it('should log the showMask() call', () => {
      parentMethods.showMask();

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called showMask()');
    });

    it('should log the hideMask() call', () => {
      parentMethods.hideMask();

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called hideMask()');
    });

    it('should log the showBusyIndicator() call', () => {
      parentMethods.showBusyIndicator();

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called showBusyIndicator()');
    });

    it('should log the hideBusyIndicator() call', () => {
      parentMethods.hideBusyIndicator();

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called hideBusyIndicator()');
    });

    it('should log the navigate() call', () => {
      const navLocationMock: commLib.NavLocation = {
        path: 'some/path',
      };

      parentMethods.navigate(navLocationMock);

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called navigate()', navLocationMock);
    });

    it('should log the updateNavLocation() call', () => {
      const navLocationMock: commLib.NavLocation = {
        path: 'some/path',
      };

      parentMethods.updateNavLocation(navLocationMock);

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called updateNavLocation()', navLocationMock);
    });

    it('should log the onError() call', () => {
      const errorMock: commLib.ClientError = {
        errorCode: 500,
        message: 'Some error',
      };

      parentMethods.onError(errorMock);

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called onError()', errorMock);
    });

    it('should log the onSessionExpired() call', () => {
      parentMethods.onSessionExpired();

      expect(logger.debug).toHaveBeenCalledWith('app \'https://some-app.com\' called onSessionExpired()');
    });
  });
});
