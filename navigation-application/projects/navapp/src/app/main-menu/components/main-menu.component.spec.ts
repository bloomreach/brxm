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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { TranslateModule } from '@ngx-translate/core';
import { of, Subject } from 'rxjs';

import { APP_BOOTSTRAPPED } from '../../bootstrap/app-bootstrapped';
import { BootstrapService } from '../../bootstrap/bootstrap.service';
import { ClientAppService } from '../../client-app/services/client-app.service';
import { BusyIndicatorService } from '../../services/busy-indicator.service';
import { NavigationService } from '../../services/navigation.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { MenuItemLinkMock } from '../models/menu-item-link.mock';
import { MenuStateService } from '../services/menu-state.service';

import { MainMenuComponent } from './main-menu.component';

describe('MainMenuComponent', () => {
  let component: MainMenuComponent;
  let fixture: ComponentFixture<MainMenuComponent>;

  let menuStateService: MenuStateService;
  const menuMock = [
    new MenuItemLinkMock({ id: 'item1' }),
    new MenuItemLinkMock({ id: 'item2' }),
  ];

  menuMock[0].navItem = {
    id: 'someId',
    appIframeUrl: 'homeAppUrl',
    appPath: 'homeAppPath',
  };

  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', {
    isMenuItemActive: undefined,
    activateMenuItem: undefined,
  });
  menuStateServiceMock.menu = menuMock;

  let qaHelperService: QaHelperService;
  const qaHelperServiceMock = {
    getMenuItemClass: jasmine.createSpy('getMenuItemClass'),
  };

  let clientAppService: ClientAppService;
  const clientAppServiceMock = {
    connectionEstablished$: of(true),
  };

  const bootstrappedSuccessful$ = new Subject();
  const bootstrapServiceMock = {
    bootstrappedSuccessful$,
  };

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const navigationServiceMock = jasmine.createSpyObj('NavigationService', [
    'navigateByNavItem',
  ]);

  let appBootstrappedResolve: () => void;
  let appBootstrappedMock: Promise<void>;

  beforeEach(() => {
    appBootstrappedMock = new Promise<void>((resolve, reject) => {
      appBootstrappedResolve = resolve;
    });

    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      declarations: [MainMenuComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: QaHelperService, useValue: qaHelperServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: BootstrapService, useValue: bootstrapServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: APP_BOOTSTRAPPED, useValue: appBootstrappedMock },
      ],
    });

    fixture = TestBed.createComponent(MainMenuComponent);

    menuStateService = fixture.debugElement.injector.get(MenuStateService);
    qaHelperService = fixture.debugElement.injector.get(QaHelperService);
    clientAppService = fixture.debugElement.injector.get(ClientAppService);

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return empty menu until app is bootstrapped', () => {
    expect(component.menuItems).toEqual([]);
  });

  it('should return all menu items when the app is bootstrapped', fakeAsync(() => {
    appBootstrappedResolve();

    tick();

    expect(component.menuItems).toEqual(menuMock);
  }));

  describe('when the app is bootstrapped', () => {
    beforeEach(async(() => {
      appBootstrappedResolve();
    }));

    it('should navigate when menu item link is clicked', () => {
      const menuItemLink = new MenuItemLinkMock();

      component.selectMenuItem(menuItemLink);

      expect(navigationServiceMock.navigateByNavItem).toHaveBeenCalledWith(menuItemLink.navItem, NavigationTrigger.Menu);
    });

    it('should not activate the home menu element until menu is emitted', () => {
      spyOn(component, 'selectMenuItem');

      expect(component.selectMenuItem).not.toHaveBeenCalled();
    });
  });
});
