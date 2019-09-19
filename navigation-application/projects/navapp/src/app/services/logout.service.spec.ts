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

import { ClientAppService } from '../client-app/services/client-app.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { GlobalSettingsService } from './global-settings.service';
import { LogoutService } from './logout.service';
import { NavConfigService } from './nav-config.service';

describe('LogoutService', () => {

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);
  const navConfigServiceMock = jasmine.createSpyObj('NavConfigService', [
    'logout',
  ]);
  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
    'logoutApps',
  ]);
  const locationMock = jasmine.createSpyObj('location', [
    'replace',
  ]);
  const navAppBaseURL = '/foo';
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: DOCUMENT, useValue: { location: locationMock } },
        { provide: GlobalSettingsService, useValue: { appSettings: { navAppBaseURL } } },
        { provide: NavConfigService, useValue: navConfigServiceMock },
      ],
    });
  });

  function expectBusyAndReplaceCalled(clientAppLogoutPromise: Promise<void>, silentLogoutPromise: Promise<void>): void {

    clientAppServiceMock.logoutApps.and.returnValue(clientAppLogoutPromise);
    navConfigServiceMock.logout.and.returnValue(silentLogoutPromise);

    const loginMessageKey = 'bar';
    const service = TestBed.get(LogoutService);
    service.logout(loginMessageKey);
    tick(1);

    expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
    expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
    expect(locationMock.replace).toHaveBeenCalledWith(`${navAppBaseURL}/?loginmessage=${loginMessageKey}`);
  }

  it('should be replace location even on silent logout rejections', fakeAsync(() => {
    expectBusyAndReplaceCalled(Promise.resolve(), Promise.reject());
  }));

  it('should be replace location even on client logout rejections', fakeAsync(() => {
    expectBusyAndReplaceCalled(Promise.reject(), Promise.resolve());
  }));
});
