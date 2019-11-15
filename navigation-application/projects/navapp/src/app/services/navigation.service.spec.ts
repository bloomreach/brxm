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

import { Location, PopStateEvent } from '@angular/common';
import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NavigationTrigger, NavItem, NavLocation } from '@bloomreach/navapp-communication';
import { TranslateService } from '@ngx-translate/core';
import { NGXLogger } from 'ngx-logger';
import { of } from 'rxjs';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { InternalError } from '../error-handling/models/internal-error';
import { NotFoundError } from '../error-handling/models/not-found-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { AppSettings } from '../models/dto/app-settings.dto';
import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { NavItemMock } from '../models/dto/nav-item.mock';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { APP_SETTINGS } from './app-settings';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';
import { NavItemService } from './nav-item.service';
import { NavigationService } from './navigation.service';
import { UrlMapperService } from './url-mapper.service';

describe('NavigationService', () => {
  let service: NavigationService;

  const basePath = '/base-path';

  let appSettingsMock: AppSettings = new AppSettingsMock({
    basePath,
  });

  const locationMock = jasmine.createSpyObj('Location', [
    'path',
    'subscribe',
    'isCurrentPathEqualTo',
    'replaceState',
    'go',
  ]);

  const navItemServiceMock = {
    navItems: [
      new NavItemMock({
        id: 'item1',
        appIframeUrl: 'http://domain.com/iframe1/url',
        appPath: 'app/path/to/home',
      }),
      new NavItemMock({
        id: 'item2',
        appIframeUrl: 'http://domain.com/iframe1/url',
        appPath: 'app/path/to/page1',
      }),
      new NavItemMock({
        id: 'item3',
        appIframeUrl: 'http://domain.com/iframe2/url',
        appPath: 'another/app/path/to/home',
      }),
    ],
  };

  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
    'getApp',
    'activateApplication',
  ]);

  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
    'activateMenuItem',
    'deactivateMenuItem',
  ]);
  menuStateServiceMock.homeMenuItem = {
    navItem: {
      appIframeUrl: 'http://domain.com/iframe1/url',
      appPath: 'app/path/to/home',
    },
  };

  const breadcrumbsServiceMock = jasmine.createSpyObj('BreadcrumbsService', [
    'setSuffix',
  ]);

  const urlMapperServiceMock = jasmine.createSpyObj('UrlMapperService', [
    'mapNavItemToBrowserUrl',
    'mapNavLocationToBrowserUrl',
    'combinePathParts',
    'trimLeadingSlash',
    'extractPathAndQueryStringAndHash',
  ]);
  urlMapperServiceMock.basePath = basePath;

  const errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
    'clearError',
    'setError',
    'setNotFoundError',
    'setInternalError',
  ]);

  const translateServiceMock = jasmine.createSpyObj('TranslateService', {
    instant: 'translated text',
  });

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

  let locationChangeFunction: (value: PopStateEvent) => undefined;

  let childApi: any;

  beforeEach(() => {
    appSettingsMock.initialPath = '/iframe1/url/app/path/to/home';

    locationMock.path.and.returnValue('');
    locationMock.isCurrentPathEqualTo.and.returnValue(false);
    locationMock.subscribe.and.callFake(cb => locationChangeFunction = cb);

    childApi = jasmine.createSpyObj('ChildApi', {
      beforeNavigation: Promise.resolve(true),
      navigate: Promise.resolve(),
    });

    clientAppServiceMock.getApp.and.returnValue(new ClientAppMock({
      api: childApi,
    }));

    urlMapperServiceMock.mapNavItemToBrowserUrl.and.callFake(
      (navItem: NavItem) => `${basePath}${new URL(navItem.appIframeUrl).pathname}/${navItem.appPath}`,
    );
    urlMapperServiceMock.trimLeadingSlash.and.callFake((value: string) => value.replace(/^\//, ''));
    urlMapperServiceMock.combinePathParts.and.callFake((...parts: string[]) => parts.map(urlMapperServiceMock.trimLeadingSlash).join('/'));
    urlMapperServiceMock.extractPathAndQueryStringAndHash.and.returnValue(['', '']);

    const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', ['show', 'hide']);
    const connectionServiceMock = {
      navigate$: of(),
      updateNavLocation$: of(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
        { provide: Location, useValue: locationMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: NavItemService, useValue: navItemServiceMock },
        { provide: UrlMapperService, useValue: urlMapperServiceMock },
        { provide: TranslateService, useValue: translateServiceMock },
        { provide: NGXLogger, useValue: loggerMock },
        NavigationService,
      ],
    });

    service = TestBed.get(NavigationService);
    appSettingsMock = TestBed.get(APP_SETTINGS);
  });

  it('should not clear the app error during initial navigation', fakeAsync(() => {
    service.initialNavigation();

    tick();

    expect(errorHandlingServiceMock.clearError).not.toHaveBeenCalled();
  }));

  describe('initialNavigation', () => {
    it('should subscribe on location changes', () => {
      service.initialNavigation();

      expect(locationMock.subscribe).toHaveBeenCalled();
    });

    it('should do the initial url navigation', () => {
      service.initialNavigation();

      expect(locationMock.replaceState).toHaveBeenCalledWith(`${basePath}/iframe1/url/app/path/to/home`, '', {});
    });

    it('should use the initialPath param', fakeAsync(() => {
      appSettingsMock.initialPath = '/iframe2/url/another/app/path/to/home';

      service.initialNavigation();

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(`${basePath}/iframe2/url/another/app/path/to/home`, '', {});
    }));

    it('should use the browser\'s location path', fakeAsync(() => {
      appSettingsMock.initialPath = undefined;
      locationMock.path.and.returnValue(`${basePath}/iframe1/url/app/path/to/page1`);

      service.initialNavigation();

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(`${basePath}/iframe1/url/app/path/to/page1`, '', {});
    }));

    it('should use the browser\'s location path with a query string', fakeAsync(() => {
      appSettingsMock.initialPath = undefined;
      locationMock.path.and.returnValue(`${basePath}/iframe1/url/app/path/to/page1?queryString=value`);

      service.initialNavigation();

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(`${basePath}/iframe1/url/app/path/to/page1?queryString=value`, '', {});
    }));

    it('should use the browser\'s location path with a hash', fakeAsync(() => {
      appSettingsMock.initialPath = undefined;
      locationMock.path.and.returnValue(`${basePath}/iframe1/url/app/path/to/page1?#hash`);

      service.initialNavigation();

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(`${basePath}/iframe1/url/app/path/to/page1?#hash`, '', {});
    }));

    it('should use the browser\'s location path with a query string and a hash', fakeAsync(() => {
      appSettingsMock.initialPath = undefined;
      locationMock.path.and.returnValue(`${basePath}/iframe1/url/app/path/to/page1?q=value#hash`);

      service.initialNavigation();

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(`${basePath}/iframe1/url/app/path/to/page1?q=value#hash`, '', {});
    }));
  });

  describe('beforeNavigation', () => {
    const navItem = new NavItemMock({
      appIframeUrl: 'http://domain.com/iframe1/url',
      appPath: 'app/path/to/page1',
    });

    let beforeNavigationResolve: (value: boolean) => void;
    let beforeNavigationReject: (reason?: any) => void;

    beforeEach(fakeAsync(() => {
      service.initialNavigation();

      tick();

      childApi.beforeNavigation.calls.reset();
      childApi.beforeNavigation.and.returnValue(new Promise((resolve, reject) => {
        beforeNavigationResolve = resolve;
        beforeNavigationReject = reject;
      }));

      childApi.navigate.calls.reset();
    }));

    it('should be called before the navigate method', () => {
      service.navigateByNavItem(navItem, NavigationTrigger.NotDefined);

      expect(childApi.beforeNavigation).toHaveBeenCalled();
      expect(childApi.navigate).not.toHaveBeenCalled();
    });

    it('should proceed to the navigate method invocation if "true" is returned', fakeAsync(() => {
      service.navigateByNavItem(navItem, NavigationTrigger.NotDefined);

      beforeNavigationResolve(true);

      tick();

      expect(childApi.navigate).toHaveBeenCalled();
    }));

    it('should prevent the navigate method invocation if "false" is returned', fakeAsync(() => {
      service.navigateByNavItem(navItem, NavigationTrigger.NotDefined);

      beforeNavigationResolve(false);

      tick();

      expect(childApi.navigate).not.toHaveBeenCalled();
    }));

    it('should set the error if the promise is rejected', fakeAsync(() => {
      const expectedError = new Error('Some error during before navigation call');

      service.navigateByNavItem(navItem, NavigationTrigger.NotDefined);

      beforeNavigationReject(expectedError);

      tick();

      expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(undefined, expectedError.message);
    }));
  });

  describe('after initial navigation', () => {
    beforeEach(fakeAsync(() => {
      service.initialNavigation();

      tick();

      locationMock.go.calls.reset();
      locationMock.replaceState.calls.reset();
      childApi.navigate.calls.reset();
      menuStateServiceMock.activateMenuItem.calls.reset();
      breadcrumbsServiceMock.setSuffix.calls.reset();
    }));

    it('should navigate by a nav item ', fakeAsync(() => {
      const navItem = new NavItemMock({
        appIframeUrl: 'http://domain.com/iframe1/url',
        appPath: 'app/path/to/page1',
      });

      service.navigateByNavItem(navItem, NavigationTrigger.NotDefined, 'some breadcrumb label');

      tick();

      expect(locationMock.go).toHaveBeenCalledWith(
        `${basePath}/iframe1/url/app/path/to/page1`,
        '',
        { breadcrumbLabel: 'some breadcrumb label' },
      );
    }));

    it('should activate the app before ChildApi.navigate() is called and activated even in a case of the error', async(() => {
      const navItem = new NavItemMock({
        appIframeUrl: 'http://domain.com/iframe1/url',
        appPath: 'app/path/to/page1',
      });

      childApi.navigate.and.returnValue(new Promise(r => {
        expect(clientAppServiceMock.activateApplication).toHaveBeenCalledWith('http://domain.com/iframe1/url');

        r();
      }));

      service.navigateByNavItem(navItem, NavigationTrigger.NotDefined);
    }));

    it('should navigate to the home page', () => {
      service.navigateToHome(NavigationTrigger.FastTravel);

      expect(locationMock.go).toHaveBeenCalledWith(
        `${basePath}/iframe1/url/app/path/to/home`,
        '',
        {},
      );
    });

    it('should update browser url when updateByNavLocation is called', fakeAsync(() => {
      urlMapperServiceMock.mapNavLocationToBrowserUrl.and.returnValue([
        '/iframe1/url/app/path/to/page1/internal/page1',
        new NavItemMock({ appIframeUrl: 'testUrl', appPath: 'testPath' }),
      ]);

      const navLocation: NavLocation = {
        path: 'app/path/to/page1/internal/page1',
        breadcrumbLabel: 'some breadcrumb label',
        addHistory: true,
      };

      service.updateByNavLocation(navLocation);

      tick();

      expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('testUrl', 'testPath');
      expect(breadcrumbsServiceMock.setSuffix).toHaveBeenCalledWith('some breadcrumb label');
      expect(locationMock.go).toHaveBeenCalledWith(
        '/iframe1/url/app/path/to/page1/internal/page1',
        '',
        { breadcrumbLabel: 'some breadcrumb label' },
      );
    }));

    it('should navigate by NavLocation', fakeAsync(() => {
      urlMapperServiceMock.mapNavLocationToBrowserUrl.and.returnValue([
        `${basePath}/iframe1/url/app/path/to/page1/internal/page1`,
        new NavItemMock(),
      ]);
      urlMapperServiceMock.extractPathAndQueryStringAndHash.and.returnValue(['internal/page1', '']);

      const navLocation: NavLocation = {
        path: 'app/path/to/page1/internal/page1',
        breadcrumbLabel: 'another breadcrumb label',
      };

      service.navigateByNavLocation(navLocation, NavigationTrigger.Menu);

      tick();

      expect(childApi.navigate).toHaveBeenCalledWith(
        { pathPrefix: '/iframe1/url', path: 'app/path/to/page1/internal/page1' },
        NavigationTrigger.Menu,
      );
      expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe1/url', 'app/path/to/page1');
      expect(breadcrumbsServiceMock.setSuffix).toHaveBeenCalledWith('another breadcrumb label');
      expect(locationMock.go).toHaveBeenCalledWith(
        `${basePath}/iframe1/url/app/path/to/page1/internal/page1`,
        '',
        { breadcrumbLabel: 'another breadcrumb label' },
      );
    }));

    it('should replace browser state when url is the same', fakeAsync(() => {
      urlMapperServiceMock.mapNavLocationToBrowserUrl.and.returnValue([
        `${basePath}/iframe1/url/app/path/to/page1/internal/page1`,
        new NavItemMock(),
      ]);

      locationMock.isCurrentPathEqualTo.and.returnValue(true);

      const navLocation: NavLocation = {
        path: 'app/path/to/page1/internal/page1',
        breadcrumbLabel: 'a breadcrumb label',
      };

      service.navigateByNavLocation(navLocation, NavigationTrigger.NotDefined);

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(
        `${basePath}/iframe1/url/app/path/to/page1/internal/page1`,
        '',
        { breadcrumbLabel: 'a breadcrumb label' },
      );
    }));

    it('should navigate on location changes', fakeAsync(() => {
      const urlChangeEvent: PopStateEvent = {
        url: `${basePath}/iframe2/url/another/app/path/to/home`,
        state: { breadcrumbLabel: 'label', flags: '{}' },
      };

      locationChangeFunction(urlChangeEvent);

      tick();

      expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe2/url', 'another/app/path/to/home');
      expect(breadcrumbsServiceMock.setSuffix).toHaveBeenCalledWith('label');
      expect(locationMock.replaceState).not.toHaveBeenCalled();
      expect(locationMock.go).not.toHaveBeenCalled();
    }));

    describe('navigateToDefaultAppPage', () => {
      it('should navigate to the default page for the app', fakeAsync(() => {
        service.navigateToDefaultAppPage(NavigationTrigger.NotDefined);

        tick();

        expect(locationMock.go).toHaveBeenCalledWith(
          `${basePath}/iframe1/url/app/path/to/home`,
          '',
          {},
        );
      }));

      describe('after updateByNavLocation invocation', () => {
        beforeEach(() => {
          urlMapperServiceMock.mapNavLocationToBrowserUrl.and.returnValue([
            `${basePath}/iframe1/url/app/path/to/page1`,
            new NavItemMock({
              id: 'item2',
              appIframeUrl: 'http://domain.com/iframe1/url',
              appPath: 'app/path/to/page1',
            }),
          ]);

          service.updateByNavLocation({
            path: 'app/path/to/page1',
          });
        });

        it('should navigate to the other default page for the app', fakeAsync(() => {
          service.navigateToDefaultAppPage(NavigationTrigger.NotDefined);

          tick();

          expect(locationMock.go).toHaveBeenCalledWith(
            `${basePath}/iframe1/url/app/path/to/page1`,
            '',
            {},
          );
        }));
      });
    });

    describe('eager state update', () => {
      const invalidNavItem = new NavItemMock({
        appIframeUrl: 'http://domain.com/some/unknown/url',
        appPath: 'app/path/to/other-page',
      });
      const validNavItem = new NavItemMock({
        appIframeUrl: 'http://domain.com/iframe1/url',
        appPath: 'app/path/to/page1',
      });

      beforeEach(() => {
        childApi.navigate.and.callFake(() => Promise.reject(new Error('Some error')));
      });

      it('should update the browser\'s url before any errors are thrown (before resolving an active route)', () => {
        const expectedError = new NotFoundError();

        service.navigateByNavItem(invalidNavItem, NavigationTrigger.NotDefined);

        expect(locationMock.go).toHaveBeenCalledWith(
          '/base-path/some/unknown/url/app/path/to/other-page',
          '',
          {},
        );
        expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
      });

      it('should activate the new appropriate menu item before calling childApi.navigate()', fakeAsync(() => {
        service.navigateByNavItem(validNavItem, NavigationTrigger.NotDefined);

        tick();

        expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe1/url', 'app/path/to/page1');
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(undefined, 'Some error');
      }));

      it('should set the breadcrumb label before calling childApi.navigate()', fakeAsync(() => {
        service.navigateByNavItem(validNavItem, NavigationTrigger.NotDefined, 'some breadcrumb label');

        tick();

        expect(breadcrumbsServiceMock.setSuffix).toHaveBeenCalledWith('some breadcrumb label');
        expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(undefined, 'Some error');
      }));
    });

    describe('error handling', () => {
      it('should clear the app error during navigation', fakeAsync(() => {
        service.navigateByNavItem(new NavItemMock(), NavigationTrigger.NotDefined, 'some breadcrumb label');

        tick();

        expect(errorHandlingServiceMock.clearError).toHaveBeenCalled();
      }));

      it('should translate the public error message if there is an unknown url', () => {
        const badNavItem = new NavItemMock({
          appIframeUrl: 'https://unknown-url.com/unknown/path',
        });

        service.navigateByNavItem(badNavItem, NavigationTrigger.NotDefined);

        expect(translateServiceMock.instant).toHaveBeenCalledWith('ERROR_UNKNOWN_URL', { url: '/base-path/unknown/path/testPath' });
      });

      it('should throw an error if the url provided is not recognizable', fakeAsync(() => {
        const expectedError = new NotFoundError('translated text');

        const badNavItem = new NavItemMock({
          appIframeUrl: 'https://unknown-url.com',
        });

        service.navigateByNavItem(badNavItem, NavigationTrigger.NotDefined);

        tick();

        expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
      }));

      it('should clear the app error on a pop state event', () => {
        const urlChangeEvent: PopStateEvent = {
          url: 'some/url',
          state: {},
        };

        locationChangeFunction(urlChangeEvent);

        expect(errorHandlingServiceMock.clearError).toHaveBeenCalled();
      });

      it('should set the error when path provided to navigateByNavLocation is not recognised', () => {
        urlMapperServiceMock.mapNavLocationToBrowserUrl.and.callFake(() => { throw new Error(); });

        const navLocation: NavLocation = {
          path: 'unknown/path',
          breadcrumbLabel: 'another breadcrumb label',
        };

        service.navigateByNavLocation(navLocation, NavigationTrigger.NotDefined);

        expect(errorHandlingServiceMock.setNotFoundError).toHaveBeenCalledWith(
          undefined,
          'An attempt to navigate was failed due to app path is not allowable: \'unknown/path\'',
        );
      });

      it('should set the error when path provided to updateByNavLocation is not recognised', () => {
        urlMapperServiceMock.mapNavLocationToBrowserUrl.and.callFake(() => { throw new Error(); });

        const navLocation: NavLocation = {
          path: 'unknown/path',
          breadcrumbLabel: 'another breadcrumb label',
        };

        service.updateByNavLocation(navLocation);

        expect(errorHandlingServiceMock.setNotFoundError).toHaveBeenCalledWith(
          undefined,
          'An attempt to update the app url was failed due to app path is not allowable: \'unknown/path\'',
        );
      });

      describe('of client apps', () => {
        const navItemToNavigate = new NavItemMock({
          appIframeUrl: 'http://domain.com/iframe1/url',
          appPath: 'app/path/to/page1',
        });

        it('should set the error when it is impossible to retrieve the app', fakeAsync(() => {
          const expectedError = new NotFoundError();

          clientAppServiceMock.getApp.and.returnValue(undefined);

          service.navigateByNavItem(navItemToNavigate, NavigationTrigger.NotDefined);

          tick();

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        }));

        it('should set the error when there is undefined api of the app', fakeAsync(() => {
          const expectedError = new InternalError();

          const app = new ClientAppMock();
          app.api = undefined;

          clientAppServiceMock.getApp.and.returnValue(app);

          service.navigateByNavItem(navItemToNavigate, NavigationTrigger.NotDefined);

          tick();

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        }));

        it('should set the error when API does not contain navigate method', fakeAsync(() => {
          clientAppServiceMock.getApp.and.returnValue(new ClientAppMock({
            api: {},
          }));

          service.navigateByNavItem(navItemToNavigate, NavigationTrigger.NotDefined);

          tick();

          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(undefined, 't.app.api.navigate is not a function');
        }));
      });
    });
  });
});
