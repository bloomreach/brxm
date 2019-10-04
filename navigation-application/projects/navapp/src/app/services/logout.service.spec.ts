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

import { Location } from '@angular/common';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { ClientAppService } from '../client-app/services/client-app.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { LogoutService } from './logout.service';
import { NavConfigService } from './nav-config.service';

describe('LogoutService', () => {
  let logoutService: LogoutService;
  let clientAppService: ClientAppService;
  let busyIndicatorService: BusyIndicatorService;
  let navConfigService: NavConfigService;

  const basePath = '/base/path';

  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', {
    logoutApps: Promise.resolve(),
  });

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const navConfigServiceMock = jasmine.createSpyObj('NavConfigService', {
    logout: Promise.resolve(),
  });

  const locationMock = jasmine.createSpyObj('Location', [
    'prepareExternalUrl',
  ]);

  const windowRefMock = {
    nativeWindow: {
      location: jasmine.createSpyObj('location', [
        'replace',
      ]),
    },
  };

  beforeEach(() => {
    locationMock.prepareExternalUrl.and.callFake(url => `${basePath}${url}`);

    TestBed.configureTestingModule({
      providers: [
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: Location, useValue: locationMock },
        { provide: WindowRef, useValue: windowRefMock },
      ],
    });

    logoutService = TestBed.get(LogoutService);
    clientAppService = TestBed.get(ClientAppService);
    busyIndicatorService = TestBed.get(BusyIndicatorService);
    navConfigService = TestBed.get(NavConfigService);
  });

  it('should replace the location even on silent logout rejections', fakeAsync(() => {
    const loginMessageKey = 'bar';

    logoutService.logout(loginMessageKey);
    tick(1);

    expect(busyIndicatorService.show).toHaveBeenCalled();
    expect(busyIndicatorService.hide).toHaveBeenCalled();
    expect(windowRefMock.nativeWindow.location.replace).toHaveBeenCalledWith(`${basePath}/?loginmessage=${loginMessageKey}`);
  }));

  it('should replace the location even on client logout rejections', fakeAsync(() => {
    clientAppServiceMock.logoutApps.and.returnValue(Promise.reject());
    const loginMessageKey = 'bar';

    logoutService.logout(loginMessageKey);
    tick(1);

    expect(busyIndicatorService.show).toHaveBeenCalled();
    expect(busyIndicatorService.hide).toHaveBeenCalled();
    expect(windowRefMock.nativeWindow.location.replace).toHaveBeenCalledWith(`${basePath}/?loginmessage=${loginMessageKey}`);
  }));
});
