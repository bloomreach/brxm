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
import { NavItem, NavLocation } from '@bloomreach/navapp-communication';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { InternalError } from '../error-handling/models/internal-error';
import { NotFoundError } from '../error-handling/models/not-found-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItemMock } from '../models/dto/nav-item.mock';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';
import { NavigationService } from './navigation.service';
import { UrlMapperService } from './url-mapper.service';

describe('NavigationService', () => {
  let service: NavigationService;

  const locationMock = jasmine.createSpyObj('Location', [
    'path',
    'subscribe',
    'isCurrentPathEqualTo',
    'replaceState',
    'go',
  ]);

  const navConfigServiceMock = {
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
  ]);

  const globalSettingsServiceMock: any = {
    appSettings: {
      navAppBaseURL: 'https://some-domain.com/iframe1/url',
      initialPath: '/app/path/to/home',
    },
  };

  const errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
    'clearError',
    'setError',
    'setNotFoundError',
    'setInternalError',
  ]);

  let locationChangeFunction: (value: PopStateEvent) => undefined;

  let childApi: any;

  beforeEach(() => {
    locationMock.path.and.returnValue('');
    locationMock.isCurrentPathEqualTo.and.returnValue(false);
    locationMock.subscribe.and.callFake(cb => locationChangeFunction = cb);

    childApi = jasmine.createSpyObj('ChildApi', {
      navigate: Promise.resolve(),
    });

    clientAppServiceMock.getApp.and.returnValue(new ClientAppMock({
      api: childApi,
    }));

    urlMapperServiceMock.mapNavItemToBrowserUrl.and.callFake(
      (navItem: NavItem) => `${new URL(navItem.appIframeUrl).pathname}/${navItem.appPath}`,
    );
    urlMapperServiceMock.trimLeadingSlash.and.callFake((value: string) => value.replace(/^\//, ''));
    urlMapperServiceMock.combinePathParts.and.callFake((...parts: string[]) => parts.map(urlMapperServiceMock.trimLeadingSlash).join('/'));

    TestBed.configureTestingModule({
      providers: [
        NavigationService,
        { provide: Location, useValue: locationMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceMock },
        { provide: UrlMapperService, useValue: urlMapperServiceMock },
        { provide: GlobalSettingsService, useValue: globalSettingsServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
      ],
    });

    service = TestBed.get(NavigationService);
  });

  it('should not clear the app error during initial navigation', fakeAsync(() => {
    service.initialNavigation();

    tick();

    expect(errorHandlingServiceMock.clearError).not.toHaveBeenCalled();
  }));

  describe('initialNavigation', () => {
    beforeEach(async(() => {
      service.initialNavigation();
    }));

    it('should subscribe on location changes', () => {
      expect(locationMock.subscribe).toHaveBeenCalled();
    });

    it('should do the initial url navigation', () => {
      expect(locationMock.replaceState).toHaveBeenCalledWith('/iframe1/url/app/path/to/home', '', { flags: '{}' });
    });
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
      const flags = { forceRefresh: false };

      service.navigateByNavItem(navItem, 'some breadcrumb label', flags);

      tick();

      expect(locationMock.go).toHaveBeenCalledWith(
        '/iframe1/url/app/path/to/page1',
        '',
        { breadcrumbLabel: 'some breadcrumb label', flags: '{"forceRefresh":false}' },
      );
    }));

    it('should navigate to the default page for the app', fakeAsync(() => {
      service.navigateToDefaultCurrentAppPage();

      tick();

      expect(locationMock.go).toHaveBeenCalledWith(
        '/iframe1/url/app/path/to/home',
        '',
        { flags: '{"forceRefresh":true}' },
      );
    }));

    it('should update browser url when updateByNavLocation is called', fakeAsync(() => {
      urlMapperServiceMock.mapNavLocationToBrowserUrl.and.returnValue([
        '/iframe1/url/app/path/to/page1/internal/page1',
        new NavItemMock({ appIframeUrl: 'testUrl', appPath: 'testPath' }),
      ]);

      const navLocation: NavLocation = {
        path: 'app/path/to/page1/internal/page1',
        breadcrumbLabel: 'some breadcrumb label',
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
        '/iframe1/url/app/path/to/page1/internal/page1',
        new NavItemMock(),
      ]);

      const navLocation: NavLocation = {
        path: 'app/path/to/page1/internal/page1',
        breadcrumbLabel: 'another breadcrumb label',
      };

      service.navigateByNavLocation(navLocation);

      tick();

      expect(childApi.navigate).toHaveBeenCalledWith({ path: 'app/path/to/page1/internal/page1' }, {});
      expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe1/url', 'app/path/to/page1');
      expect(breadcrumbsServiceMock.setSuffix).toHaveBeenCalledWith('another breadcrumb label');
      expect(locationMock.go).toHaveBeenCalledWith(
        '/iframe1/url/app/path/to/page1/internal/page1',
        '',
        { breadcrumbLabel: 'another breadcrumb label', flags: '{}' },
      );
    }));

    it('should replace browser state when url is the same', fakeAsync(() => {
      urlMapperServiceMock.mapNavLocationToBrowserUrl.and.returnValue([
        '/iframe1/url/app/path/to/page1/internal/page1',
        new NavItemMock(),
      ]);

      locationMock.isCurrentPathEqualTo.and.returnValue(true);

      const navLocation: NavLocation = {
        path: 'app/path/to/page1/internal/page1',
        breadcrumbLabel: 'a breadcrumb label',
      };

      service.navigateByNavLocation(navLocation);

      tick();

      expect(locationMock.replaceState).toHaveBeenCalledWith(
        '/iframe1/url/app/path/to/page1/internal/page1',
        '',
        { breadcrumbLabel: 'a breadcrumb label', flags: '{}' },
      );
    }));

    it('should navigate on location changes', fakeAsync(() => {
      const urlChangeEvent: PopStateEvent = {
        url: '/iframe2/url/another/app/path/to/home',
        state: { breadcrumbLabel: 'label', flags: '{}' },
      };

      locationChangeFunction(urlChangeEvent);

      tick();

      expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe2/url', 'another/app/path/to/home');
      expect(breadcrumbsServiceMock.setSuffix).toHaveBeenCalledWith('label');
      expect(locationMock.replaceState).not.toHaveBeenCalled();
      expect(locationMock.go).not.toHaveBeenCalled();
    }));

    describe('errors handling', () => {
      it('should clear the app error during navigation', fakeAsync(() => {
        service.navigateByNavItem(new NavItemMock(), 'some breadcrumb label');

        tick();

        expect(errorHandlingServiceMock.clearError).toHaveBeenCalled();
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

        service.navigateByNavLocation(navLocation);

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

          service.navigateByNavItem(navItemToNavigate);

          tick();

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        }));

        it('should set the error when there is undefined api of the app', fakeAsync(() => {
          const expectedError = new InternalError();

          const app = new ClientAppMock();
          app.api = undefined;

          clientAppServiceMock.getApp.and.returnValue(app);

          service.navigateByNavItem(navItemToNavigate);

          tick();

          expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
        }));

        it('should set the error when API does not contain navigate method', fakeAsync(() => {
          clientAppServiceMock.getApp.and.returnValue(new ClientAppMock({
            api: {},
          }));

          service.navigateByNavItem(navItemToNavigate);

          tick();

          expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(undefined, 'app.api.navigate is not a function');
        }));
      });
    });
  });
});
