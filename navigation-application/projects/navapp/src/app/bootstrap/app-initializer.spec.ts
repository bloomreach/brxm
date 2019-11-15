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

import { fakeAsync, tick } from '@angular/core/testing';

import { appInitializer } from './app-initializer';
import * as loadNavItemsModule from './load-nav-items';
import * as scheduleAppBootstrappingModule from './schedule-app-bootstrapping';

describe('appInitializer', () => {
  const navConfigServiceMock: any = {};

  const bootstrapServiceMock = jasmine.createSpyObj('BootstrapService', [
    'bootstrap',
  ]);

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
    'setCriticalError',
  ]);

  const authServiceMock = jasmine.createSpyObj('AuthService', [
    'loginAllResources',
  ]);

  beforeEach(() => {
    spyOnProperty(loadNavItemsModule, 'loadNavItems', 'get').and.returnValue(() => Promise.resolve());
  });

  it('should initialize the app', fakeAsync(() => {
    spyOnProperty(scheduleAppBootstrappingModule, 'scheduleAppBootstrapping', 'get').and.returnValue(() => Promise.resolve());

    let initialized = false;

    appInitializer(
      authServiceMock,
      navConfigServiceMock,
      bootstrapServiceMock,
      busyIndicatorServiceMock,
      errorHandlingServiceMock,
    )().then(() => initialized = true);

    tick();

    expect(initialized).toBeTruthy();
  }));

  it('should set the critical error', fakeAsync(() => {
    spyOnProperty(scheduleAppBootstrappingModule, 'scheduleAppBootstrapping', 'get').and.returnValue(() => Promise.reject());

    appInitializer(
      authServiceMock,
      navConfigServiceMock,
      bootstrapServiceMock,
      busyIndicatorServiceMock,
      errorHandlingServiceMock,
    )();

    tick();

    expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
      'ERROR_UNABLE_TO_LOAD_CONFIGURATION',
      'navConfigService.init is not a function',
    );
  }));
});
