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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItemMock } from '../models/nav-item.mock';
import { AuthService } from '../services/auth.service';
import { BusyIndicatorService } from '../services/busy-indicator.service';
import { NavConfigService } from '../services/nav-config.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';

import { BootstrapService } from './bootstrap.service';

describe('BootstrapService', () => {
  let service: BootstrapService;

  const navItemDtosMock = [
    { id: '1', appIframeUrl: 'https://some-url', appPath: 'home/path' },
    { id: '2', appIframeUrl: 'https://some-url', appPath: 'some/path' },
    { id: '3', appIframeUrl: 'https://another-url', appPath: 'another/path' },
  ];

  const navItemsMock = [
    new NavItemMock({ id: '1' }),
    new NavItemMock({ id: '2' }),
    new NavItemMock({ id: '3' }),
  ];

  let appConnectedSubject: Subject<ClientApp>;

  let authServiceMock: jasmine.SpyObj<AuthService>;
  let navConfigServiceMock: jasmine.SpyObj<NavConfigService>;
  let navItemServiceMock: jasmine.SpyObj<NavItemService>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;
  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;
  let navigationServiceMock: jasmine.SpyObj<NavigationService>;
  let busyIndicatorServiceMock: jasmine.SpyObj<BusyIndicatorService>;
  let errorHandlingServiceMock: jasmine.SpyObj<ErrorHandlingService>;

  beforeEach(() => {
    appConnectedSubject = new Subject();

    authServiceMock = jasmine.createSpyObj('AuthService', {
      loginAllResources: Promise.resolve(),
    });

    navConfigServiceMock = jasmine.createSpyObj('NavConfigService', {
      init: navItemDtosMock,
    });

    navItemServiceMock = jasmine.createSpyObj('NavItemService', {
      registerNavItemDtos: navItemsMock,
      activateNavItems: undefined,
    });

    clientAppServiceMock = jasmine.createSpyObj('ClientAppService', {
      init: Promise.resolve(),
    });
    (clientAppServiceMock as any).appConnected$ = appConnectedSubject.asObservable();

    menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
      'init',
    ]);

    navigationServiceMock = jasmine.createSpyObj('NavigationService', {
      init: undefined,
      initialNavigation: Promise.resolve(),
    });

    busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
      'setCriticalError',
      'setInternalError',
    ]);

    TestBed.configureTestingModule({
      providers: [
        BootstrapService,
        { provide: AuthService, useValue: authServiceMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: NavItemService, useValue: navItemServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
      ],
    });

    service = TestBed.get(BootstrapService);
  });

  describe('if everything goes well', () => {
    let bootstrapped: boolean;

    beforeEach(async(() => {
      bootstrapped = false;

      service.bootstrap().then(() => bootstrapped = true);
    }));

    it('should complete successfully', () => {
      expect(bootstrapped).toBeTruthy();
    });

    it('should show the busy indicator', () => {
      expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
    });

    it('should hide the busy indicator', () => {
      expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
      expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
    });

    it('should perform silent login', () => {
      expect(authServiceMock.loginAllResources).toHaveBeenCalled();
    });

    it('should perform silent login before fetching nav items', () => {
      expect(authServiceMock.loginAllResources).toHaveBeenCalledBefore(navConfigServiceMock.init);
    });

    it('should register fetched nav item DTOs', () => {
      expect(navItemServiceMock.registerNavItemDtos).toHaveBeenCalledWith(navItemDtosMock);
    });

    it('should initialize ClientAppService', () => {
      expect(clientAppServiceMock.init).toHaveBeenCalledWith(navItemsMock);
    });

    it('should initialize MenuStateService', () => {
      expect(menuStateServiceMock.init).toHaveBeenCalledWith(navItemsMock);
    });

    it('should initialize NavigationService', () => {
      expect(navigationServiceMock.init).toHaveBeenCalledWith(navItemsMock);
    });

    it('should perform initial navigation', () => {
      expect(navigationServiceMock.initialNavigation).toHaveBeenCalled();
    });

    it('should activate nav items for the connected app', () => {
      const expected = 'https://some-url';

      appConnectedSubject.next(new ClientApp(expected, {}));

      expect(navItemServiceMock.activateNavItems).toHaveBeenCalledWith(expected);
    });
  });

  describe('if something goes wrong', () => {
    describe('and silent login has failed', () => {
      beforeEach(async(() => {
        authServiceMock.loginAllResources.and.callFake(() => Promise.reject(new Error('silent login has failed')));

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
          'ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN',
          'silent login has failed',
        );
      });
    });

    describe('and nav item DTOs fetching has failed', () => {
      beforeEach(async(() => {
        navConfigServiceMock.init.and.callFake(() => Promise.reject(
          new Error('nav item DTOs fetching has failed'),
        ));

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
          'ERROR_UNABLE_TO_LOAD_CONFIGURATION',
          'nav item DTOs fetching has failed',
        );
      });
    });

    describe('and registration of nav item DTOs thrown an exception', () => {
      beforeEach(async(() => {
        navConfigServiceMock.init.and.callFake(() => {
          throw new Error('registration of nav item DTOs fetching has failed');
        });

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
          'ERROR_UNABLE_TO_LOAD_CONFIGURATION',
          'registration of nav item DTOs fetching has failed',
        );
      });
    });

    describe('and ClientAppService initialization has thrown an exception', () => {
      beforeEach(async(() => {
        clientAppServiceMock.init.and.callFake(() => {
          throw new Error('ClientAppService initialization error');
        });

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'ClientAppService initialization error',
        );
      });
    });

    describe('and ClientAppService initialization has failed', () => {
      beforeEach(async(() => {
        clientAppServiceMock.init.and.callFake(() => Promise.reject(new Error('some error')));

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'some error',
        );
      });
    });

    describe('and MenuStateService initialization has thrown an exception', () => {
      beforeEach(async(() => {
        menuStateServiceMock.init.and.callFake(() => {
          throw new Error('MenuStateService initialization error');
        });

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'MenuStateService initialization error',
        );
      });
    });

    describe('and NavigationService initialization has thrown an exception', () => {
      beforeEach(async(() => {
        navigationServiceMock.init.and.callFake(() => {
          throw new Error('NavigationService initialization error');
        });

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'NavigationService initialization error',
        );
      });
    });

    describe('and initial navigation has thrown an exception', () => {
      beforeEach(async(() => {
        navigationServiceMock.initialNavigation.and.callFake(() => {
          throw new Error('initial navigation error');
        });

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'initial navigation error',
        );
      });
    });

    describe('and initial navigation has failed', () => {
      beforeEach(async(() => {
        navigationServiceMock.initialNavigation.and.callFake(() => Promise.reject(new Error('some error')));

        service.bootstrap();
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'some error',
        );
      });
    });

    describe('error', () => {
      it('should be set if an exception object is provided', fakeAsync(() => {
        authServiceMock.loginAllResources.and.callFake(() => {
          throw new Error('error');
        });

        service.bootstrap();

        tick();

        expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
          'ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN',
          'error',
        );
      }));

      it('should be set if a string is provided as a rejection reason', fakeAsync(() => {
        authServiceMock.loginAllResources.and.callFake(() => Promise.reject('error'));

        service.bootstrap();

        tick();

        expect(errorHandlingServiceMock.setCriticalError).toHaveBeenCalledWith(
          'ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN',
          'error',
        );
      }));
    });
  });
});
