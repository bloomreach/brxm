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

import { async } from '@angular/core/testing';

import { NavItemMock } from '../models/nav-item.mock';

import { appInitializer } from './app-initializer';

describe('appInitializer', () => {
  const navItemsMock = [
    new NavItemMock({ id: '1' }),
    new NavItemMock({ id: '2' }),
  ];

  const authServiceMock = jasmine.createSpyObj('AuthService', [
    'loginAllResources',
  ]);

  const navConfigServiceMock = jasmine.createSpyObj('NavConfigService', {
    init: Promise.resolve(navItemsMock),
  });

  const navItemServiceMock = jasmine.createSpyObj('NavItemService', [
    'registerNavItemDtos',
  ]);

  const bootstrapServiceMock = jasmine.createSpyObj('BootstrapService', [
    'bootstrap',
  ]);

  const errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
    'setCriticalError',
  ]);

  const callAppInitializer = () => appInitializer(
    authServiceMock,
    navConfigServiceMock,
    navItemServiceMock,
    bootstrapServiceMock,
    errorHandlingServiceMock,
  )();

  let initialized: boolean;

  describe('when services initialized successfully', () => {
    beforeEach(async(() => {
      initialized = false;

      callAppInitializer().then(() => initialized = true);
    }));

    it('should log in all resources', () => {
      expect(authServiceMock.loginAllResources).toHaveBeenCalled();
    });

    it('should initialize NavConfigService', () => {
      expect(navConfigServiceMock.init).toHaveBeenCalled();
    });

    it('should register fetched nav items', () => {
      expect(navItemServiceMock.registerNavItemDtos).toHaveBeenCalledWith(navItemsMock);
    });

    it('should bootstrap the rest services', () => {
      expect(bootstrapServiceMock.bootstrap).toHaveBeenCalled();
    });

    it('should initialize the app', () => {
      expect(initialized).toBeTruthy();
    });
  });

  describe('when an error occurred', () => {
    beforeEach(async(() => {
      authServiceMock.loginAllResources.and.callFake(() => {
        throw new Error('some error');
      });

      callAppInitializer();
    }));

    it('should set the critical error', () => {
      expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
        'ERROR_UNABLE_TO_LOAD_CONFIGURATION',
        'some error',
      );
    });
  });
});
