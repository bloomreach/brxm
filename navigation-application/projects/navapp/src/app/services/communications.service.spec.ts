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

/*import { TestBed } from '@angular/core/testing';
import { ChildPromisedApi } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services';
import { MenuBuilderService, MenuStateService, MenuStructureService } from '../main-menu/services';

import { CommunicationsService } from './communications.service';
import { NavConfigService } from './nav-config.service';
import { OverlayService } from './overlay.service';

describe('CommunicationsService', () => {
  let clientAppService: jasmine.SpyObj<ClientAppService>;
  let navConfigService: jasmine.SpyObj<NavConfigService>;
  let menuStateService: jasmine.SpyObj<MenuStateService>;
  let overLayService: jasmine.SpyObj<OverlayService>;
  let service: CommunicationsService;

  const menuItems = [
    {
      id: 'hippo-perspective-dashboardperspective',
      caption: 'Home',
      appId: 'http://localhost:8080/cms/?iframe',
      appPath: 'hippo-perspective-dashboardperspective',
    },
    {
      id: 'hippo-perspective-channelmanagerperspective',
      caption: 'Experience manager',
      icon: 'experience-manager',
      appId: 'http://localhost:8080/cms/?iframe',
      appPath: 'hippo-perspective-channelmanagerperspective',
    },
    {
      id: 'hippo-perspective-browserperspective',
      caption: 'Content',
      icon: 'documents',
      appId: 'http://localhost:8080/cms/?iframe',
      appPath: 'hippo-perspective-browserperspective',
    },
    {
      id: 'hippo-perspective-reportsperspective',
      caption: 'Content Reports',
      appId: 'http://localhost:8080/cms/?iframe',
      appPath: 'hippo-perspective-reportsperspective',
    },
    {
      id: 'hippo-perspective-adminperspective',
      caption: 'System',
      appId: 'http://localhost:8080/cms/?iframe',
      appPath: 'hippo-perspective-adminperspective',
    },
  ];

  function setup(): {
    communicationsService: CommunicationsService;
    overlayService: OverlayService;
    clientAppService: ClientAppService;
    api: ChildPromisedApi;
  } {
    const overlayService = jasmine.createSpyObj({
      enable: jasmine.createSpy(),
      disable: jasmine.createSpy(),
    });

    const api = jasmine.createSpyObj({
      navigate: Promise.resolve(),
    });

    const clientAppService = jasmine.createSpyObj('clientAppService', {
      getApp: {
        api,
      },
      activateApplication: jasmine.createSpy(),
    });

    TestBed.configureTestingModule({
      providers: [
        CommunicationsService,
        { provide: OverlayService, useValue: overlayService },
        { provide: ClientAppService, useValue: clientAppService },
      ],
    });

    return {
      communicationsService: TestBed.get(CommunicationsService),
      overlayService: TestBed.get(OverlayService),
      clientAppService: TestBed.get(ClientAppService),
      api,
    };
  }

  beforeEach(() => {
    const api = jasmine.createSpyObj({
      navigate: Promise.resolve(),
    });

    const clientAppServiceSpy = jasmine.createSpyObj('clientAppService', {
      getApp: {
        api,
      },
      activateApplication: jasmine.createSpy(),
    });
    const navConfigServiceSpy = jasmine.createSpyObj('clientAppService', {
      getApp: {
        api,
      },
      activateApplication: jasmine.createSpy(),
    });
    const overLayServiceSpy = jasmine.createSpyObj('OverlayService', ['disable', 'enable']);
    const menuStateServiceSpy = jasmine.createSpyObj('MenuStateService', ['setActiveItem', 'setActiveItemAndNavigate', '$menu']);

    TestBed.configureTestingModule({
      providers: [
        CommunicationsService,
        { provide: ClientAppService, useValue: clientAppServiceSpy },
        { provide: NavConfigService, useValue: navConfigServiceSpy },
        { provide: MenuStateService, useValue: menuStateServiceSpy },
        { provide: OverlayService, useValue: overLayServiceSpy },
      ],
    });

    clientAppService = TestBed.get(ClientAppService);
    navConfigService = TestBed.get(NavConfigService);
    menuStateService = TestBed.get(MenuStateService);
    overLayService = TestBed.get(OverlayService);

    service = TestBed.get(CommunicationsService);
  });

  it('should navigate', () => {
    const { communicationsService, clientAppService, api } = setup();
    const url = 'url';
    const path = 'test/test';

    expect(communicationsService).toBeTruthy();

    communicationsService.navigate(url, path).then(() => {
      expect(api.navigate).toHaveBeenCalledWith({ path });
      expect(clientAppService.activateApplication).toHaveBeenCalledWith(url);
    });
  });

  describe('.getApiMethods()', () => {
    describe('.navigate', () => {

      it('should select the associated menu item and navigate when the item is found', () => {
        appToNavAppService.menu$ = of(menuItems);
        appToNavAppService.parentApiMethods.navigate({path: 'hippo-perspective-adminperspective'});
        const findElement = menuItems.filter(item => item.appPath === 'hippo-perspective-adminperspective')[0];
        expect(menuStateService.setActiveItemAndNavigate).toHaveBeenCalledWith(findElement);
      });

      it('should log and error when the item is not found', () => {
        console.error = jasmine.createSpy('error');
        appToNavAppService.menu$ = of(menuItems);
        const location: NavLocation = {path: 'not found'};
        appToNavAppService.parentApiMethods.navigate(location);
        expect(console.error).toHaveBeenCalledWith(`Cannot find associated menu item for Navlocation:{${JSON.stringify(location)}}`);
      });
    });
    describe('.updateNavLocation', () => {
      it('should select the associated menu item when the item is found', () => {
        appToNavAppService.menu$ = of(menuItems);
        appToNavAppService.parentApiMethods.updateNavLocation({path: 'hippo-perspective-adminperspective'});
        const findElement = menuItems.filter(item => item.appPath === 'hippo-perspective-adminperspective')[0];
        expect(menuStateService.setActiveItem).toHaveBeenCalledWith(findElement);
      });

      it('should log and error when the item is not found', () => {
        console.error = jasmine.createSpy('error');
        appToNavAppService.menu$ = of(menuItems);
        const location: NavLocation = {path: 'not found'};
        appToNavAppService.parentApiMethods.navigate(location);
        expect(console.error).toHaveBeenCalledWith(`Cannot find associated menu item for Navlocation:{${JSON.stringify(location)}}`);
      });
    });
    describe('.showMask', () => {
      it('should enable the overlay', () => {
        appToNavAppService.parentApiMethods.showMask();
        expect(overLayService.enable).toHaveBeenCalled();
      });
    });
    describe('.hideMask', () => {
      it('should disable the overlay', () => {
        appToNavAppService.parentApiMethods.hideMask();
        expect(overLayService.disable).toHaveBeenCalled();
      });
    });
  });
});*/
