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
import { ReplaySubject } from 'rxjs';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { CommunicationsService } from './communications.service';
import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

describe('CommunicationsService', () => {
  let service: CommunicationsService;
  let clientAppService: ClientAppService;
  let navConfigService: NavConfigService;
  let menuStateService: MenuStateService;
  let breadcrumbsService: BreadcrumbsService;
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

  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
    'setActiveItem',
    'setActiveItemAndNavigate',
    'activateMenuItem',
  ]);

  const breadcrumbsServiceMock = jasmine.createSpyObj('BreadcrumbsService', [
    'setSuffix',
    'clearSuffix',
  ]);

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
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

    menuStateServiceMock.activeMenuItem$ = activeMenuItem$;
    activeMenuItem$.next({
      appId: 'testMenuItem',
      appPath: 'testpath',
    });

    TestBed.configureTestingModule({
      providers: [
        CommunicationsService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceMock },
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
      ],
    });

    service = TestBed.get(CommunicationsService);
    clientAppService = TestBed.get(ClientAppService);
    navConfigService = TestBed.get(NavConfigService);
    menuStateService = TestBed.get(MenuStateService);
    breadcrumbsService = TestBed.get(BreadcrumbsService);
    overlayService = TestBed.get(OverlayService);
    busyIndicatorService = TestBed.get(BusyIndicatorService);
  });

  describe('client api methods', () => {
    describe('navigate', () => {
      it('should get the client app and communicate the Location to be navigated to', fakeAsync(() => {
        service.navigate('testId', 'testPath');

        expect(
          clientAppService.getApp('testId').api.navigate,
        ).toHaveBeenCalledWith({ path: 'testPath' }, undefined);

        tick();

        expect(clientAppService.activateApplication).toHaveBeenCalledWith(
          'testId',
        );
      }));

      it('should show the busy indicator', () => {
        service.navigate('testId', 'testPath');

        expect(busyIndicatorService.show).toHaveBeenCalled();
        expect(busyIndicatorService.hide).toHaveBeenCalled();
      });
    });

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
      });

      it('should logout all apps', () => {
        service.logout()
          .then(
            () => expect(navConfigService.logout).toHaveBeenCalled(),
          )
          .catch(
            () => fail('Expected navConfigService.logout to have been called'),
          );
      });

      it('should show the busy indicator', () => {
        service.logout();

        expect(busyIndicatorService.show).toHaveBeenCalled();
        expect(busyIndicatorService.hide).toHaveBeenCalled();
      });
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
        });

        expect(menuStateService.activateMenuItem).toHaveBeenCalledWith(
          path,
          path,
        );
      });

      it('should select the associated menu item and navigate when the item is found', () => {
        const path = 'some-perspective';
        clientAppServiceMock.activeApp = clientApps[1];

        navConfigServiceMock.findNavItem.and.returnValue({
          id: path,
        });

        service.parentApiMethods.navigate({
          path,
        });

        expect(menuStateService.activateMenuItem).toHaveBeenCalledWith(
          path,
          path,
        );
      });

      it('should log an error if the item is not found', () => {
        const path = 'some-perspective';
        spyOn(console, 'error');
        navConfigServiceMock.findNavItem.and.returnValue(undefined);
        service.parentApiMethods.navigate({
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

        service.parentApiMethods.updateNavLocation(location);

        expect(menuStateService.activateMenuItem).toHaveBeenCalledWith(
          clientAppService.activeApp.url,
          path,
        );
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
  });

  describe('breadcrumbs', () => {
    beforeEach(() => {
      (breadcrumbsService.clearSuffix as jasmine.Spy).calls.reset();
    });

    it('should be set from parent API navigate', () => {
      const path = 'some-perspective';
      const breadcrumbLabel = 'some breadcrumb label';
      spyOn(service, 'navigate');
      navConfigServiceMock.findNavItem.and.returnValue({
        id: path,
      });

      service.parentApiMethods.navigate({
        path,
        breadcrumbLabel,
      });

      expect(breadcrumbsService.setSuffix).toHaveBeenCalledWith(breadcrumbLabel);
    });

    it('should be set from parent API updateNavLocation', () => {
      const path = 'some-perspective';
      const breadcrumbLabel = 'some breadcrumb label';

      service.parentApiMethods.updateNavLocation({
        path,
        breadcrumbLabel,
      });

      expect(breadcrumbsService.setSuffix).toHaveBeenCalledWith(breadcrumbLabel);
    });

    it('should be cleared when a new active menu item is selected', () => {
      (breadcrumbsService.clearSuffix as jasmine.Spy).calls.reset();

      activeMenuItem$.next({
        appId: 'testMenuItem',
        appPath: 'testpath',
      });

      expect(breadcrumbsService.clearSuffix).toHaveBeenCalled();
    });

    it('should be cleared when navigated to the default page', () => {
      service.navigateToDefaultPage();

      expect(breadcrumbsService.clearSuffix).toHaveBeenCalled();
    });
  });
});
