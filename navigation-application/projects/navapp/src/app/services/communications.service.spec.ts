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

import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NavLocation } from '@bloomreach/navapp-communication';
import { of } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';

import { CommunicationsService } from './communications.service';
import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

describe('CommunicationsService', () => {
  let clientAppService: ClientAppService;
  let navConfigService: NavConfigService;
  let menuStateService: MenuStateService;
  let overlayService: OverlayService;
  let communicationsService: CommunicationsService;

  const overlayServiceMock = jasmine.createSpyObj('OverlayService', [
    'enable',
    'disable',
  ]);

  const clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
    'getApp',
    'activateApplication',
  ]);

  const navConfigServiceMock = jasmine.createSpyObj('NavConfigService', [
    'findNavItem',
  ]);

  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
    'setActiveItem',
    'setActiveItemAndNavigate',
    'activateMenuItem',
  ]);

  beforeEach(() => {
    const parentApiMock = jasmine.createSpyObj('parentApi', {
      navigate: Promise.resolve(),
      updateSite: Promise.resolve(),
      logout: Promise.resolve(),
    });

    const clientApps: ClientApp[] = [
      new ClientApp('hippo-perspective-adminperspective'),
      new ClientApp('hippo-perspective-reportsperspective'),
    ];

    clientApps[0].api = { ...parentApiMock };
    clientApps[1].api = { ...parentApiMock };

    clientAppServiceMock.getApp.and.returnValue(clientApps[0]);
    clientAppServiceMock.activeApp = clientApps[0];
    clientAppServiceMock.apps$ = of(clientApps);
    clientAppServiceMock.appsWithSitesSupport = [
      clientApps[0],
      {
        id: 'test2',
        api: jasmine.createSpyObj('parentApi', {
          navigate: Promise.resolve(),
          updateSite: Promise.resolve(),
        }),
      },
    ];

    menuStateServiceMock.activeMenuItem$ = of({
      appId: 'testMenuItem',
      appPath: 'testpath',
    });

    TestBed.configureTestingModule({
      providers: [
        CommunicationsService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: OverlayService, useValue: overlayServiceMock },
      ],
    });

    clientAppService = TestBed.get(ClientAppService);
    navConfigService = TestBed.get(NavConfigService);
    menuStateService = TestBed.get(MenuStateService);
    overlayService = TestBed.get(OverlayService);
    communicationsService = TestBed.get(CommunicationsService);
  });

  describe('client api methods', () => {
    describe('navigate', () => {
      it('should get the client app and communicate the Location to be navigated to', fakeAsync(() => {
        communicationsService.navigate('testId', 'testPath');

        expect(
          clientAppService.getApp('testId').api.navigate,
        ).toHaveBeenCalledWith({ path: 'testPath' });

        tick();

        expect(clientAppService.activateApplication).toHaveBeenCalledWith(
          'testId',
        );
      }));
    });

    describe('updateSite', () => {
      it('should get the client app and communicate the site id', () => {
        communicationsService.updateSite(1337);
        expect(
          clientAppService.getApp('testId').api.updateSite,
        ).toHaveBeenCalledWith(1337);
      });

      it('should trigger to all supporting client apps to update their site', fakeAsync(() => {
        communicationsService.updateSite(1337);

        tick();

        expect(
          clientAppService.appsWithSitesSupport[0].api.updateSite,
        ).toHaveBeenCalledTimes(1);
        expect(
          clientAppService.appsWithSitesSupport[1].api.updateSite,
        ).toHaveBeenCalled();
      }));
    });

    describe('logout', () => {
      it('should logout all apps', () => {
        communicationsService.logout();

        clientAppService.apps$.subscribe(apps => {
          apps.forEach(app => {
            expect(app.api.logout).toHaveBeenCalled();
          });
        });
      });
    });
  });

  describe('parent api methods', () => {
    describe('.navigate', () => {
      it('should select the associated menu item and navigate when the item is found', () => {
        const path = 'hippo-perspective-adminperspective';
        spyOn(communicationsService, 'navigate');
        navConfigServiceMock.findNavItem.and.returnValue({
          id: path,
        });

        communicationsService.parentApiMethods.navigate({
          path,
        });

        expect(menuStateService.activateMenuItem).toHaveBeenCalledWith(
          path,
          path,
        );
        expect(communicationsService.navigate).toHaveBeenCalledWith(path, path);
      });

      it('should log an error if the item is not found', () => {
        const path = 'hippo-perspective-adminperspective';
        spyOn(console, 'error');
        navConfigServiceMock.findNavItem.and.returnValue(undefined);
        communicationsService.parentApiMethods.navigate({
          path,
        });

        expect(console.error).toHaveBeenCalled();
      });
    });

    describe('.updateNavLocation', () => {
      it('should select the associated menu item based on active app id and path', () => {
        const path = 'test/path';
        const location: NavLocation = {
          path,
        };

        communicationsService.parentApiMethods.updateNavLocation(location);

        expect(menuStateService.activateMenuItem).toHaveBeenCalledWith(
          clientAppService.activeApp.id,
          path,
        );
      });
    });

    describe('.showMask', () => {
      it('should enable the overlay', () => {
        communicationsService.parentApiMethods.showMask();
        expect(overlayService.enable).toHaveBeenCalled();
      });
    });

    describe('.hideMask', () => {
      it('should disable the overlay', () => {
        communicationsService.parentApiMethods.hideMask();
        expect(overlayService.disable).toHaveBeenCalled();
      });
    });
  });
});
