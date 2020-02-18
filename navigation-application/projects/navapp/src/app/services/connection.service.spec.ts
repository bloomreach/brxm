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

import { DOCUMENT } from '@angular/common';
import { Renderer2, RendererFactory2 } from '@angular/core';
import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import * as commLib from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { UserSettingsMock } from '../models/dto/user-settings.mock';

import { APP_SETTINGS } from './app-settings';
import { ChildConnection, ConnectionService } from './connection.service';
import { USER_SETTINGS } from './user-settings';

// That allows to get rid of the "createElement is deprecated" warning
interface DocumentMock extends Document {
  createElement: (tagName: string) => HTMLElement;
}

describe('ConnectionService', () => {
  let service: ConnectionService;
  let logger: NGXLogger;

  let documentMock: jasmine.SpyObj<DocumentMock>;
  let loggerMock: jasmine.SpyObj<NGXLogger>;
  let rendererFactoryMock: jasmine.SpyObj<RendererFactory2>;
  let rendererMock: jasmine.SpyObj<Renderer2>;
  let connectToChildSpy: jasmine.Spy;
  let parentMethods: commLib.ParentApi;

  const childApiMock: commLib.ChildPromisedApi = {};

  beforeEach(() => {
    const appSettingsMock = new AppSettingsMock();
    const userSettingsMock = new UserSettingsMock();

    documentMock = jasmine.createSpyObj('Document', {
      createElement: { style: {} },
    });
    documentMock.body = {} as any;

    loggerMock = jasmine.createSpyObj('NGXLogger', [
      'debug',
    ]);

    rendererMock = jasmine.createSpyObj('Renderer2', [
      'appendChild',
      'removeChild',
    ]);
    rendererFactoryMock = jasmine.createSpyObj('RendererFactory2', {
      createRenderer: rendererMock,
    });

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
        { provide: DOCUMENT, useValue: documentMock },
        { provide: RendererFactory2, useValue: rendererFactoryMock },
        { provide: NGXLogger, useValue: loggerMock },
      ],
    });

    service = TestBed.get(ConnectionService);
    logger = TestBed.get(NGXLogger);
  });

  it('should create the renderer', () => {
    expect(rendererFactoryMock.createRenderer).toHaveBeenCalledWith(undefined, undefined);
  });

  describe('createConnection', () => {
    const url = 'http://localhost/testUrl';
    let connection: ChildConnection;

    beforeEach(async () => {
      connection = await service.createConnection(url);
    });

    it('should create an iframe', () => {
      expect(documentMock.createElement).toHaveBeenCalledWith('iframe');
    });

    it('should make the create iframe hidden and add to the DOM', () => {
      expect(rendererMock.appendChild).toHaveBeenCalledWith(documentMock.body, {
        src: url,
        style: {
          visibility: 'hidden',
          position: 'absolute',
          width: '1px',
          height: '1px',
        },
      });
    });

    it('should create a connection', () => {
      expect(connection.url).toBe(url);
    });

    it('should not create the same connection twice but return en existing one from the pool', async () => {
      documentMock.createElement.calls.reset();
      rendererMock.appendChild.calls.reset();

      const newConnection = await service.createConnection(url);

      expect(newConnection).toBe(connection);
      expect(documentMock.createElement).not.toHaveBeenCalled();
      expect(rendererMock.appendChild).not.toHaveBeenCalled();
    });
  });

  describe('when a connection to a hidden iframe is created', () => {
    const url = 'http://localhost/testUrl';

    beforeEach(async () => {
      return service.createConnection(url);
    });

    describe('removeConnection', () => {
      it('should remove the connection', () => {
        service.removeConnection(url);

        expect(rendererMock.removeChild).toHaveBeenCalledWith(documentMock.body, {
          src: url,
          style: {
            visibility: 'hidden',
            position: 'absolute',
            width: '1px',
            height: '1px',
          },
        });
      });

      it('should throw an exception if a connection does not exist', () => {
        expect(() => service.removeConnection('unknown-url')).toThrowError('Connection to \'unknown-url\' does not exist');
      });
    });
  });

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
