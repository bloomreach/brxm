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
import { NavLocation } from '@bloomreach/navapp-communication';
import { ReplaySubject } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { CommunicationsService } from './communications.service';
import { NavConfigService } from './nav-config.service';
import { NavigationService } from './navigation.service';
import { OverlayService } from './overlay.service';

describe('CommunicationsService', () => {
  let service: CommunicationsService;
  let clientAppService: ClientAppService;
  let navConfigService: NavConfigService;
  let overlayService: OverlayService;
  let busyIndicatorService: BusyIndicatorService;

  let clientApps: ClientApp[];

  const activeMenuItem$ = new ReplaySubject(1);

  const overlayServiceMock = jasmine.createSpyObj('OverlayService', [
    'enable',
    'disable',
  ]);

  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
    'getApp',
    'activateApplication',
    'logoutApps',
  ]);

  const navConfigServiceMock = jasmine.createSpyObj('NavConfigService', [
    'findNavItem',
    'logout',
  ]);

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const navigationServiceMock = jasmine.createSpyObj('NavigationService', [
    'navigateByNavLocation',
    'updateByNavLocation',
  ]);

  const locationMock = jasmine.createSpyObj('Location', [
    'replace',
  ]);

  beforeEach(() => {
    const childApiMock = jasmine.createSpyObj('parentApi', {
      navigate: Promise.resolve(),
      updateSelectedSite: Promise.resolve(),
      logout: Promise.resolve(),
    });

    clientApps = [
      new ClientApp('some-perspective', {}),
      new ClientApp('another-perspective', {}),
    ];

    clientApps[0].api = { ...childApiMock };
    delete clientApps[0].api.updateSelectedSite;
    clientApps[1].api = { ...childApiMock };

    clientAppServiceMock.getApp.and.returnValue(clientApps[1]);
    clientAppServiceMock.activeApp = clientApps[1];
    clientAppServiceMock.apps = clientApps;

    navConfigServiceMock.findNavItem.and.returnValue({
      id: 'some-id',
      appIframeUrl: 'some-perspective',
      appPath: 'some-path',
    });

    activeMenuItem$.next({
      appId: 'testMenuItem',
      appPath: 'testpath',
    });

    TestBed.configureTestingModule({
      providers: [
        CommunicationsService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: DOCUMENT, useValue: { location: locationMock }  },
      ],
    });

    service = TestBed.get(CommunicationsService);
    clientAppService = TestBed.get(ClientAppService);
    navConfigService = TestBed.get(NavConfigService);
    overlayService = TestBed.get(OverlayService);
    busyIndicatorService = TestBed.get(BusyIndicatorService);
  });

  describe('client api methods', () => {
    describe('updateSelectedSite', () => {
      it('should get the client app and communicate the site id', () => {
        service.updateSelectedSite({ accountId: 10, siteId: 1337 });
        expect(
          clientAppService.getApp('testId').api.updateSelectedSite,
        ).toHaveBeenCalledWith({ accountId: 10, siteId: 1337 });
      });

      it('should trigger to all supporting client apps to update their site', fakeAsync(() => {
        service.updateSelectedSite({ accountId: 10, siteId: 1337 });

        tick();

        expect(
          clientApps[1].api.updateSelectedSite,
        ).toHaveBeenCalledTimes(1);
      }));

      it('should show the busy indicator', () => {
        service.updateSelectedSite({ accountId: 10, siteId: 1337 });

        expect(busyIndicatorService.show).toHaveBeenCalled();
        expect(busyIndicatorService.hide).toHaveBeenCalled();
      });
    });

    describe('logout', () => {
      beforeAll(() => {
        clientAppServiceMock.logoutApps.and.returnValue(Promise.resolve());
        navConfigServiceMock.logout.and.returnValue(Promise.resolve());
      });
      it('should replace the browser location', fakeAsync(() => {
        let url: string;
        locationMock.replace.and.callFake((urlFromCall: string) => {
          url = urlFromCall;
        });
        service.logout('testMessageKey');
        tick(1);
        expect(busyIndicatorService.show).toHaveBeenCalled();
        expect(busyIndicatorService.hide).toHaveBeenCalled();
        expect(locationMock.replace).toHaveBeenCalled();
        expect(url).toContain('loginmessage=testMessageKey');
      }));
    });
  });

  describe('parent api methods', () => {
    describe('.navigate', () => {
      it('should select the associated menu item when the item is found', () => {
        const path = 'some-perspective';

        navConfigServiceMock.findNavItem.and.returnValue({
          id: path,
        });

        service.parentApiMethods.navigate({
          path,
          breadcrumbLabel: 'some breadcrumb label',
        });

        expect(navigationServiceMock.navigateByNavLocation).toHaveBeenCalled();
      });
    });

    describe('.updateNavLocation', () => {
      it('should update the location', () => {
        const path = 'test/path';
        const location: NavLocation = {
          path,
        };

        service.parentApiMethods.updateNavLocation(location);

        expect(navigationServiceMock.updateByNavLocation).toHaveBeenCalled();
      });
    });

    describe('.showMask', () => {
      it('should enable the overlay', () => {
        service.parentApiMethods.showMask();
        expect(overlayService.enable).toHaveBeenCalled();
      });
    });

    describe('.hideMask', () => {
      it('should disable the overlay', () => {
        service.parentApiMethods.hideMask();
        expect(overlayService.disable).toHaveBeenCalled();
      });
    });

    describe('.onSessionExpired', () => {
      it('should replace document location, even if logout fails', fakeAsync(() => {

        clientAppServiceMock.logoutApps.and.returnValue(Promise.resolve());
        navConfigServiceMock.logout.and.returnValue(Promise.reject('fake a failing silent logout'));

        service.parentApiMethods.onSessionExpired();
        tick(1);
        expect(busyIndicatorService.show).toHaveBeenCalled();
        expect(busyIndicatorService.hide).toHaveBeenCalled();
        expect(locationMock.replace).toHaveBeenCalled();
      }));
    });
  });
});
