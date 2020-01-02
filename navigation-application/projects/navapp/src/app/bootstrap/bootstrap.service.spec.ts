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

import { async, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItemMock } from '../models/nav-item.mock';
import { BusyIndicatorService } from '../services/busy-indicator.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';

import { BootstrapService } from './bootstrap.service';

describe('BootstrapService', () => {
  let service: BootstrapService;

  const navItemsMock = [
    new NavItemMock({ id: '1' }),
    new NavItemMock({ id: '2' }),
    new NavItemMock({ id: '3' }),
  ];

  let appConnectedSubject: Subject<ClientApp>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;

  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;

  let navigationServiceMock: jasmine.SpyObj<NavigationService>;

  let navItemServiceMock: jasmine.SpyObj<NavItemService>;

  let busyIndicatorServiceMock: jasmine.SpyObj<BusyIndicatorService>;

  let errorHandlingServiceMock: jasmine.SpyObj<ErrorHandlingService>;

  beforeEach(() => {
    appConnectedSubject = new Subject();
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

    navItemServiceMock = jasmine.createSpyObj('NavItemService', [
      'activateNavItems',
    ]);

    busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
      'setInternalError',
    ]);

    TestBed.configureTestingModule({
      providers: [
        BootstrapService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: NavItemService, useValue: navItemServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
      ],
    });

    service = TestBed.get(BootstrapService);
  });

  describe('if everything goes well', () => {
    beforeEach(async(() => {
      service.bootstrap(navItemsMock);
    }));

    it('should show the busy indicator', () => {
      expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
    });

    it('should hide the busy indicator', () => {
      expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
      expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
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
    describe('and ClientAppService initialization has thrown an exception', () => {
      beforeEach(async(() => {
        clientAppServiceMock.init.and.callFake(() => {
          throw new Error('ClientAppService initialization error');
        });

        service.bootstrap(navItemsMock);
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'Error: ClientAppService initialization error',
        );
      });
    });

    describe('and ClientAppService initialization has failed', () => {
      beforeEach(async(() => {
        clientAppServiceMock.init.and.callFake(() => Promise.reject(new Error('some error')));

        service.bootstrap(navItemsMock);
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'Error: some error',
        );
      });
    });

    describe('and MenuStateService initialization has thrown an exception', () => {
      beforeEach(async(() => {
        menuStateServiceMock.init.and.callFake(() => {
          throw new Error('MenuStateService initialization error');
        });

        service.bootstrap(navItemsMock);
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'Error: MenuStateService initialization error',
        );
      });
    });

    describe('and NavigationService initialization has thrown an exception', () => {
      beforeEach(async(() => {
        navigationServiceMock.init.and.callFake(() => {
          throw new Error('NavigationService initialization error');
        });

        service.bootstrap(navItemsMock);
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'Error: NavigationService initialization error',
        );
      });
    });

    describe('and initial navigation has thrown an exception', () => {
      beforeEach(async(() => {
        navigationServiceMock.initialNavigation.and.callFake(() => {
          throw new Error('initial navigation error');
        });

        service.bootstrap(navItemsMock);
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'Error: initial navigation error',
        );
      });
    });

    describe('and initial navigation has failed', () => {
      beforeEach(async(() => {
        navigationServiceMock.initialNavigation.and.callFake(() => Promise.reject(new Error('some error')));

        service.bootstrap(navItemsMock);
      }));

      it('should set the error', () => {
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
          'ERROR_INITIALIZATION',
          'Error: some error',
        );
      });
    });
  });
});
