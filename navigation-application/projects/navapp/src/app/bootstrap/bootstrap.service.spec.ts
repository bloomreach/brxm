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

import { fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { NavigationTrigger, Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Subject } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { CriticalError } from '../error-handling/models/critical-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItemMock } from '../models/nav-item.mock';
import { AuthService } from '../services/auth.service';
import { BusyIndicatorService } from '../services/busy-indicator.service';
import { MainLoaderService } from '../services/main-loader.service';
import { NavConfigService } from '../services/nav-config.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';
import { SiteService } from '../services/site.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BootstrapService } from './bootstrap.service';

describe('BootstrapService', () => {
  let service: BootstrapService;

  const navItemDtosMock = [
    { id: '1', appIframeUrl: 'https://some-url', appPath: 'home/path' },
    { id: '2', appIframeUrl: 'https://some-url', appPath: 'some/path' },
    { id: '3', appIframeUrl: 'https://another-url', appPath: 'another/path' },
  ];

  const sitesMock: Site[] = [
    {
      siteId: 1,
      accountId: 456,
      isNavappEnabled: true,
      name: 'myTestSite',
      subGroups: [
        {
          siteId: 2,
          accountId: 123,
          isNavappEnabled: true,
          name: 'myTestSite2',
        },
      ],
    },
    {
      siteId: 3,
      accountId: 890,
      isNavappEnabled: true,
      name: 'myTestSite3',
    },
  ];

  const selectedSiteIdMock: SiteId = {
    siteId: 2,
    accountId: 123,
  };

  const navItemsMock = [
    new NavItemMock({ id: '1' }),
    new NavItemMock({ id: '2' }),
    new NavItemMock({ id: '3' }),
  ];

  let appConnectedSubject: Subject<ClientApp>;
  let selectedSiteSubject: Subject<Site>;

  let authServiceMock: jasmine.SpyObj<AuthService>;
  let navConfigServiceMock: jasmine.SpyObj<NavConfigService>;
  let navItemServiceMock: jasmine.SpyObj<NavItemService>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;
  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;
  let navigationServiceMock: jasmine.SpyObj<NavigationService>;
  let mainLoaderServiceMock: jasmine.SpyObj<MainLoaderService>;
  let busyIndicatorServiceMock: jasmine.SpyObj<BusyIndicatorService>;
  let siteServiceMock: jasmine.SpyObj<SiteService>;
  let errorHandlingServiceMock: jasmine.SpyObj<ErrorHandlingService>;
  let windowRefMock: WindowRef;
  let loggerMock: jasmine.SpyObj<NGXLogger>;

  beforeEach(() => {
    appConnectedSubject = new Subject();

    authServiceMock = jasmine.createSpyObj('AuthService', {
      loginAllResources: Promise.resolve(),
    });

    navConfigServiceMock = jasmine.createSpyObj('NavConfigService', {
      fetchNavigationConfiguration: ({
        navItems: navItemDtosMock,
        sites: sitesMock,
        selectedSiteId: selectedSiteIdMock,
      }),
      refetchNavItems: navItemDtosMock,
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
      navigateToHome: Promise.resolve(),
      reload: Promise.resolve(),
    });

    mainLoaderServiceMock = jasmine.createSpyObj('MainLoaderService', [
      'show',
      'hide',
    ]);

    busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    siteServiceMock = jasmine.createSpyObj('SiteService', [
      'init',
    ]);
    selectedSiteSubject = new Subject();
    (siteServiceMock as any).selectedSite$ = selectedSiteSubject;

    errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
      'setInternalError',
      'setError',
    ]);

    windowRefMock = {
      nativeWindow: {
        location: {
          reload: jasmine.createSpy('reload'),
        },
      },
    } as any;

    loggerMock = jasmine.createSpyObj('NGXLogger', [
      'debug',
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
        { provide: MainLoaderService, useValue: mainLoaderServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: SiteService, useValue: siteServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
        { provide: WindowRef, useValue: windowRefMock },
        { provide: NGXLogger, useValue: loggerMock },
      ],
    });

    service = TestBed.inject(BootstrapService);
  });

  describe('bootstrap', () => {
    describe('if everything goes well', () => {
      let bootstrapped: boolean;

      beforeEach(waitForAsync(() => {
        bootstrapped = false;

        service.bootstrap().then(() => bootstrapped = true);
      }));

      it('should complete successfully', () => {
        expect(bootstrapped).toBeTruthy();
      });

      it('should show the main loader', () => {
        expect(mainLoaderServiceMock.show).toHaveBeenCalled();
      });

      it('should show the busy indicator', () => {
        expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
      });

      it('should hide the main loader', () => {
        expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
        expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
      });

      it('should hide the busy indicator', () => {
        expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
        expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
      });

      it('should perform silent login', () => {
        expect(authServiceMock.loginAllResources).toHaveBeenCalled();
      });

      it('should fetch the configuration', () => {
        expect(navConfigServiceMock.fetchNavigationConfiguration).toHaveBeenCalled();
      });

      it('should perform silent login before fetching the configuration', () => {
        expect(authServiceMock.loginAllResources).toHaveBeenCalledBefore(navConfigServiceMock.fetchNavigationConfiguration);
      });

      it('should initialize site service', () => {
        expect(siteServiceMock.init).toHaveBeenCalledWith(sitesMock, selectedSiteIdMock);
      });

      it('should register fetched nav item DTOs', () => {
        expect(navItemServiceMock.registerNavItemDtos).toHaveBeenCalledWith(navItemDtosMock);
      });

      it('should initialize MenuStateService', () => {
        expect(menuStateServiceMock.init).toHaveBeenCalledWith(navItemsMock);
      });

      it('should initialize NavigationService', () => {
        expect(navigationServiceMock.init).toHaveBeenCalledWith(navItemsMock);
      });

      it('should initialize ClientAppService', () => {
        expect(clientAppServiceMock.init).toHaveBeenCalledWith(navItemsMock);
      });

      it('should perform initial navigation', () => {
        expect(navigationServiceMock.initialNavigation).toHaveBeenCalled();
      });

      it('should activate nav items for the connected app', () => {
        const expected = 'https://some-url';

        appConnectedSubject.next(new ClientApp(expected, {}));

        expect(navItemServiceMock.activateNavItems).toHaveBeenCalledWith(expected);
      });

      describe('logging', () => {
        it('should log the start of bootstrapping process', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith('Bootstrapping the application');
        });

        it('should log starting of silent login', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith('Performing silent login');
        });

        it('should log successful silent login', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith('Silent login has done successfully');
        });

        it('should log starting of fetching the configuration', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith('Fetching the application\'s configuration');
        });

        it('should log if configuration was fetched successfully', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith('The application configuration has been fetched successfully');
        });
      });
    });

    describe('if something goes wrong', () => {
      beforeEach(() => {
        mainLoaderServiceMock.show.calls.reset();
        mainLoaderServiceMock.hide.calls.reset();
      });

      describe('and silent login has failed', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          authServiceMock.loginAllResources.and.callFake(() => Promise.reject(new Error('silent login has failed')));

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          const expectedError = new CriticalError('ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN', 'silent login has failed');

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and nav item DTOs fetching has failed', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          navConfigServiceMock.fetchNavigationConfiguration.and.callFake(() => Promise.reject(
            new Error('nav item DTOs fetching has failed'),
          ));

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          const expectedError = new CriticalError(
            'ERROR_UNABLE_TO_LOAD_CONFIGURATION',
            'nav item DTOs fetching has failed',
          );

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and nav item DTOs fetching resolved with an empty array', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          navConfigServiceMock.fetchNavigationConfiguration.and.returnValue({
            navItems: [],
            sites: [],
            selectedSite: undefined,
          });

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          const expectedError = new CriticalError('ERROR_UNABLE_TO_LOAD_CONFIGURATION');

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and registration of nav item DTOs thrown an exception', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          navItemServiceMock.registerNavItemDtos.and.callFake(() => {
            throw new Error('registration of nav item DTOs fetching has failed');
          });

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'registration of nav item DTOs fetching has failed',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and ClientAppService initialization has thrown an exception', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          clientAppServiceMock.init.and.callFake(() => {
            throw new Error('ClientAppService initialization error');
          });

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'ClientAppService initialization error',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and ClientAppService initialization has failed', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          clientAppServiceMock.init.and.callFake(() => Promise.reject(new Error('some error')));

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'some error',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and MenuStateService initialization has thrown an exception', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          menuStateServiceMock.init.and.callFake(() => {
            throw new Error('MenuStateService initialization error');
          });

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'MenuStateService initialization error',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and NavigationService initialization has thrown an exception', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          navigationServiceMock.init.and.callFake(() => {
            throw new Error('NavigationService initialization error');
          });

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'NavigationService initialization error',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and initial navigation has thrown an exception', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          navigationServiceMock.initialNavigation.and.callFake(() => {
            throw new Error('initial navigation error');
          });

          service.bootstrap();

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'initial navigation error',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('and initial navigation has failed', () => {
        let bootstrapped: boolean;

        beforeEach(async () => {
          bootstrapped = false;

          navigationServiceMock.initialNavigation.and.callFake(() => Promise.reject(new Error('some error')));

          await service.bootstrap();

          bootstrapped = true;
        });

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'some error',
          );
        });

        it('should bootstrap the app', () => {
          expect(bootstrapped).toBeTruthy();
        });

        it('should hide the main loader', () => {
          expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
          expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
        });

        it('should hide the busy indicator', () => {
          expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
          expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
        });
      });

      describe('error', () => {
        it('should be set if an exception object is provided', fakeAsync(() => {
          const expectedError = new CriticalError('ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN', 'error');

          authServiceMock.loginAllResources.and.callFake(() => {
            throw new Error('error');
          });

          service.bootstrap();

          tick();

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        }));

        it('should be set if a string is provided as a rejection reason', fakeAsync(() => {
          const expectedError = new CriticalError('ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN', 'error');

          authServiceMock.loginAllResources.and.callFake(() => Promise.reject('error'));

          service.bootstrap();

          tick();

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        }));
      });
    });
  });

  describe('reinitialize', () => {
    describe('if everything goes well', () => {
      let reinitialized: boolean;

      const newNavItemDtosMock = [
        { id: '4', appIframeUrl: 'https://some-new-url', appPath: 'home/path' },
        { id: '5', appIframeUrl: 'https://some-new-url', appPath: 'some/path' },
        { id: '6', appIframeUrl: 'https://another-new-url', appPath: 'another/path' },
      ];

      const newNavItemsMock = [
        new NavItemMock({ id: '4' }),
        new NavItemMock({ id: '5' }),
        new NavItemMock({ id: '6' }),
      ];

      beforeEach(waitForAsync(() => {
        reinitialized = false;

        navConfigServiceMock.refetchNavItems.and.returnValue(newNavItemDtosMock);
        navItemServiceMock.registerNavItemDtos.and.returnValue(newNavItemsMock);

        service.reinitialize().then(() => reinitialized = true);
      }));

      it('should complete successfully', () => {
        expect(reinitialized).toBeTruthy();
      });

      it('should show the main loader', () => {
        expect(mainLoaderServiceMock.show).toHaveBeenCalled();
      });

      it('should show the busy indicator', () => {
        expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
      });

      it('should hide the main loader', () => {
        expect(mainLoaderServiceMock.hide).toHaveBeenCalled();
        expect(mainLoaderServiceMock.show).toHaveBeenCalledBefore(mainLoaderServiceMock.hide);
      });

      it('should hide the busy indicator', () => {
        expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
        expect(busyIndicatorServiceMock.show).toHaveBeenCalledBefore(busyIndicatorServiceMock.hide);
      });

      it('should refetch nav item DTOs', () => {
        expect(navConfigServiceMock.refetchNavItems).toHaveBeenCalled();
      });

      it('should register fetched nav item DTOs', () => {
        expect(navItemServiceMock.registerNavItemDtos).toHaveBeenCalledWith(newNavItemDtosMock);
      });

      it('should initialize MenuStateService', () => {
        expect(menuStateServiceMock.init).toHaveBeenCalledWith(newNavItemsMock);
      });

      it('should initialize NavigationService', () => {
        expect(navigationServiceMock.init).toHaveBeenCalledWith(newNavItemsMock);
      });

      it('should initialize ClientAppService', () => {
        expect(clientAppServiceMock.init).toHaveBeenCalledWith(newNavItemsMock);
      });

      it('should reload the page (SPA reload without actual reload)', () => {
        expect(navigationServiceMock.reload).toHaveBeenCalled();
      });

      describe('logging', () => {
        it('should log the start of reinitializing process', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith('Reinitializing the application');
        });
      });
    });

    describe('if something goes wrong', () => {
      describe('and nav item DTOs fetching has failed', () => {
        beforeEach(waitForAsync(() => {
          navConfigServiceMock.refetchNavItems.and.callFake(() => Promise.reject(
            new Error('nav item DTOs fetching has failed'),
          ));

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'nav item DTOs fetching has failed',
          );
        });
      });

      describe('and registration of nav item DTOs thrown an exception', () => {
        beforeEach(waitForAsync(() => {
          navItemServiceMock.registerNavItemDtos.and.callFake(() => {
            throw new Error('registration of nav item DTOs fetching has failed');
          });

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'registration of nav item DTOs fetching has failed',
          );
        });
      });

      describe('and ClientAppService initialization has thrown an exception', () => {
        beforeEach(waitForAsync(() => {
          clientAppServiceMock.init.and.callFake(() => {
            throw new Error('ClientAppService initialization error');
          });

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'ClientAppService initialization error',
          );
        });
      });

      describe('and ClientAppService initialization has failed', () => {
        beforeEach(waitForAsync(() => {
          clientAppServiceMock.init.and.callFake(() => Promise.reject(new Error('some error')));

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'some error',
          );
        });
      });

      describe('and MenuStateService initialization has thrown an exception', () => {
        beforeEach(waitForAsync(() => {
          menuStateServiceMock.init.and.callFake(() => {
            throw new Error('MenuStateService initialization error');
          });

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'MenuStateService initialization error',
          );
        });
      });

      describe('and NavigationService initialization has thrown an exception', () => {
        beforeEach(waitForAsync(() => {
          navigationServiceMock.init.and.callFake(() => {
            throw new Error('NavigationService initialization error');
          });

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'NavigationService initialization error',
          );
        });
      });

      describe('and reloading of the page has thrown an exception', () => {
        beforeEach(waitForAsync(() => {
          navigationServiceMock.reload.and.callFake(() => {
            throw new Error('reload of the page error');
          });

          service.reinitialize();
        }));

        it('should navigate to the home page', () => {
          expect(navigationServiceMock.navigateToHome).toHaveBeenCalled();
        });
      });

      describe('and reloading of the page has failed', () => {
        beforeEach(waitForAsync(() => {
          navigationServiceMock.reload.and.callFake(() => Promise.reject(new Error('some error')));

          service.reinitialize();
        }));

        it('should navigate to the home page', () => {
          expect(navigationServiceMock.navigateToHome).toHaveBeenCalled();
        });
      });

      describe('and navigation to home page has thrown an exception', () => {
        beforeEach(waitForAsync(() => {
          navigationServiceMock.reload.and.callFake(() => {
            throw new Error('reload of the page error');
          });
          navigationServiceMock.navigateToHome.and.callFake(() => {
            throw new Error('navigation to home page error');
          });

          service.reinitialize();
        }));

        it('should set the error', () => {
          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
            'ERROR_INITIALIZATION',
            'navigation to home page error',
          );
        });
      });

      describe('and navigation to home page has failed', () => {
        beforeEach(waitForAsync(() => {
          navigationServiceMock.reload.and.callFake(() => {
            throw new Error('reload of the page error');
          });
          navigationServiceMock.navigateToHome.and.callFake(() => Promise.reject(new Error('some error')));

          service.reinitialize();
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
          clientAppServiceMock.init.and.callFake(() => {
            throw new Error('error');
          });

          service.reinitialize();

          tick();

          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith('ERROR_INITIALIZATION', 'error');
        }));

        it('should be set if a string is provided as a rejection reason', fakeAsync(() => {
          clientAppServiceMock.init.and.callFake(() => Promise.reject('error'));

          service.reinitialize();

          tick();

          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith('ERROR_INITIALIZATION', 'error');
        }));
      });
    });
  });
});
