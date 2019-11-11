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
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import * as comLib from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { UserSettingsMock } from '../models/dto/user-settings.mock';

import { APP_SETTINGS } from './app-settings';
import { ConnectionService } from './connection.service';
import { USER_SETTINGS } from './user-settings';

describe('ConnectionService', () => {
  let connectionService: ConnectionService;

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

  beforeEach(() => {
    const appSettingsMock = new AppSettingsMock();
    const userSettingsMock = new UserSettingsMock();
    const rendererMock = {
      createRenderer: () => ({
        appendChild: () => { },
        removeChild: () => { },
      }),
    };

    spyOnProperty(comLib, 'connectToChild').and.returnValue(() => Promise.resolve());

    TestBed.configureTestingModule({
      providers: [
        ConnectionService,
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: USER_SETTINGS, useValue: userSettingsMock },
        { provide: RendererFactory2, useValue: rendererMock },
        { provide: NGXLogger, useValue: loggerMock },
      ],
    });

    connectionService = TestBed.get(ConnectionService);
  });

  it('should create a connection', fakeAsync(() => {
    const url = 'testUrl';

    connectionService
      .createConnection(url)
      .then(connection => {
        expect(connection.url.includes(url)).toBe(true);
      });

    tick();
  }));

  it('should remove a connection', fakeAsync(() => {
    const url = 'testUrl';
    connectionService
      .createConnection(url)
      .then(connection => {
        connectionService.removeConnection(connection.url);
        expect(connectionService.getConnection(connection.url)).toBeUndefined();
      });

    tick();
  }));
});
