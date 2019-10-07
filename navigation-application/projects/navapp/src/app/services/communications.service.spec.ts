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

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NavLocation } from '@bloomreach/navapp-communication';
import { ReplaySubject } from 'rxjs';

import { ClientErrorCodes } from '../../../../navapp-communication/src/lib/api';
import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { GlobalSettingsMock } from '../models/dto/global-settings.mock';

import { BusyIndicatorService } from './busy-indicator.service';
import { CommunicationsService } from './communications.service';
import { GlobalSettingsService } from './global-settings.service';
import { LogoutService } from './logout.service';
import { NavConfigService } from './nav-config.service';
import { NavigationService } from './navigation.service';
import { OverlayService } from './overlay.service';
import { UserActivityService } from './user-activity.service';

describe('CommunicationsService', () => {
  let service: CommunicationsService;
  let clientAppService: ClientAppService;
  let navConfigService: NavConfigService;
  let overlayService: OverlayService;
  let busyIndicatorService: BusyIndicatorService;

  let clientApps: ClientApp[];

  const activeMenuItem$ = new ReplaySubject(1);

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
    'getApp',
    'activateApplication',
    'logoutApps',
  ]);

  const logoutServiceMock = jasmine.createSpyObj('LogoutService', [
    'logout',
  ]);

  const navigationServiceMock = jasmine.createSpyObj('NavigationService', [
    'navigateByNavLocation',
    'updateByNavLocation',
  ]);

  const overlayServiceMock = jasmine.createSpyObj('OverlayService', [
    'enable',
    'disable',
  ]);

  const userActivityServiceMock = jasmine.createSpyObj('UserActivityService', [
    'broadcastUserActivity',
  ]);

  const globalSettingsServiceMock = new GlobalSettingsMock();

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

    activeMenuItem$.next({
      appId: 'testMenuItem',
      appPath: 'testpath',
    });

    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [
        CommunicationsService,
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: LogoutService, useValue: logoutServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: UserActivityService, useValue: userActivityServiceMock },
        { provide: GlobalSettingsService, useValue: globalSettingsServiceMock },
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
  });

  describe('parent api methods', () => {
    describe('.navigate', () => {
      it('should select the associated menu item when the item is found', () => {
        const path = 'some-perspective';

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
      it('should call logout when onSessionExpired is invoked', fakeAsync(() => {
        service.parentApiMethods.onSessionExpired();
        tick(1);
        expect(logoutServiceMock.logout).toHaveBeenCalled();
      }));
    });

    describe('.onError', () => {
      it('should logout if not authorized', () => {
        const errorCode = ClientErrorCodes.NotAuthorizedError;
        service.parentApiMethods.onError({ errorCode });
        expect(logoutServiceMock.logout).toHaveBeenCalledWith(errorCode.toString());
      });
      it('should not logout on any other client errors', () => {
        logoutServiceMock.logout.calls.reset();
        Object.keys(ClientErrorCodes)
          .map(code => ClientErrorCodes[code])
          .filter(errorCode => ClientErrorCodes.NotAuthorizedError !== errorCode)
          .forEach(errorCode =>
            service.parentApiMethods.onError({ errorCode, message: 'some other client error' }));
        expect(logoutServiceMock.logout.calls.count()).toBe(0);
      });
    });

    describe('.onUserActivity', () => {
      it('should broadcast the use activity', () => {
        service.parentApiMethods.onUserActivity();

        expect(userActivityServiceMock.broadcastUserActivity).toHaveBeenCalled();
      });
    });
  });
});
