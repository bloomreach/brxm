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
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of } from 'rxjs';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItemMock } from '../models/dto/nav-item.mock';
import { GlobalSettingsService } from '../services/global-settings.service';
import { NavConfigService } from '../services/nav-config.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { DeepLinkingService } from './deep-linking.service';

describe('DeepLinkingService', () => {
  let service: DeepLinkingService;
  let location: Location;
  let navConfigService: NavConfigService;
  let clientAppService: ClientAppService;
  let menuStateService: MenuStateService;
  let breadcrumbsService: BreadcrumbsService;

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

  const globalSettingsServiceMock = {
    appSettings: {
      contextPath: '/context/',
    },
  } as any;

  let locationChangeFunction: (value: PopStateEvent) => undefined;

  beforeEach(() => {
    locationMock.path.and.returnValue('');
    locationMock.isCurrentPathEqualTo.and.returnValue(false);
    locationMock.subscribe.and.callFake(cb => locationChangeFunction = cb);

    TestBed.configureTestingModule({
      providers: [
        DeepLinkingService,
        { provide: Location, useValue: locationMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceMock },
        { provide: GlobalSettingsService, useValue: globalSettingsServiceMock },
      ],
    });

    service = TestBed.get(DeepLinkingService);
    location = TestBed.get(Location);
    navConfigService = TestBed.get(NavConfigService);
    clientAppService = TestBed.get(ClientAppService);
    menuStateService = TestBed.get(MenuStateService);
    breadcrumbsService = TestBed.get(BreadcrumbsService);
  });

  describe('initialNavigation', () => {
    beforeEach(() => {
      spyOn(service, 'navigateByUrl');
      locationMock.path.and.returnValue('some/initial/path?with=query&params#and-hash');
      service.initialNavigation();
    });

    it('should subscribe on location changes', () => {
      expect(location.subscribe).toHaveBeenCalled();
    });

    it('should do the initial url navigation', () => {
      expect(service.navigateByUrl).toHaveBeenCalledWith('some/initial/path?with=query&params#and-hash');
    });
  });

  describe('after initial navigation', () => {
    let childApi: any;

    beforeEach(fakeAsync(() => {
      childApi = jasmine.createSpyObj('ChildApi', {
        navigate: Promise.resolve(),
      });

      clientAppServiceMock.getApp.and.returnValue(new ClientAppMock({
        api: childApi,
      }));

      service.initialNavigation();

      tick();

      locationMock.go.calls.reset();
      locationMock.replaceState.calls.reset();
      childApi.navigate.calls.reset();
      menuStateServiceMock.activateMenuItem.calls.reset();
      breadcrumbsServiceMock.setSuffix.calls.reset();
    }));

    it('should navigate by using a client app url ', () => {
      spyOn(service, 'navigateByUrl');

      const flags = { someFlag: true };
      service.navigateByAppUrl('http://domain.com/iframe/url', 'app/url', 'some breadcrumb label', flags);

      expect(service.navigateByUrl).toHaveBeenCalledWith('context/iframe/url/app/url', 'some breadcrumb label', flags);
    });

    it('should navigate to the default page for the app', () => {
      spyOn(service, 'navigateByUrl');

      service.navigateToDefaultCurrentAppPage();

      expect(service.navigateByUrl).toHaveBeenCalledWith('context/iframe1/url/app/path/to/home', undefined, undefined);
    });

    it('should update browser url when app url is updated', () => {
      service.updateByAppUrl(
        'http://domain.com/iframe1/url',
        'app/path/to/page1/internal/page1',
        'some breadcrumb label',
      );

      expect(menuStateService.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe1/url', 'app/path/to/page1/internal/page1');
      expect(breadcrumbsService.setSuffix).toHaveBeenCalledWith('some breadcrumb label');
      expect(location.go).toHaveBeenCalledWith(
        '/context/iframe1/url/app/path/to/page1/internal/page1',
        '',
        { breadcrumbLabel: 'some breadcrumb label' },
      );
    });

    it('should navigate by url', fakeAsync(() => {
      service.navigateByUrl(
        'context/iframe2/url/another/app/path/to/home',
        'another breadcrumb label',
        { someFlag: true },
      );

      expect(childApi.navigate).toHaveBeenCalledWith({ path: 'another/app/path/to/home' }, { someFlag: true });

      tick();

      expect(menuStateService.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe2/url', 'another/app/path/to/home');
      expect(breadcrumbsService.setSuffix).toHaveBeenCalledWith('another breadcrumb label');
      expect(location.go).toHaveBeenCalledWith(
        '/context/iframe2/url/another/app/path/to/home',
        '',
        { breadcrumbLabel: 'another breadcrumb label', flags: '{"someFlag":true}' },
      );
    }));

    it('should replace browser state when url is the same', fakeAsync(() => {
      locationMock.isCurrentPathEqualTo.and.returnValue(true);

      service.navigateByUrl(
        'context/iframe1/url/app/path/to/home',
        'a breadcrumb label',
      );

      tick();

      expect(location.replaceState).toHaveBeenCalledWith(
        '/context/iframe1/url/app/path/to/home',
        '',
        { breadcrumbLabel: 'a breadcrumb label', flags: '{}' },
      );
    }));

    it('should navigate on location changes', fakeAsync(() => {
      const urlChangeEvent: PopStateEvent = {
        url: 'context/iframe2/url/another/app/path/to/home',
        state: { breadcrumbLabel: 'label', flags: '{}' },
      };

      locationChangeFunction(urlChangeEvent);

      tick();

      expect(menuStateService.activateMenuItem).toHaveBeenCalledWith('http://domain.com/iframe2/url', 'another/app/path/to/home');
      expect(breadcrumbsService.setSuffix).toHaveBeenCalledWith('label');
      expect(location.replaceState).not.toHaveBeenCalled();
      expect(location.go).not.toHaveBeenCalled();
    }));
  });
});
