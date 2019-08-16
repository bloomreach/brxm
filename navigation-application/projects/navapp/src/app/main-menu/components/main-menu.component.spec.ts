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
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';

import { ClientAppService } from '../../client-app/services/client-app.service';
import { BootstrapService } from '../../services/bootstrap.service';
import { GlobalSettingsService } from '../../services/global-settings.service';
import { QaHelperService } from '../../services/qa-helper.service';
import { MenuItemLinkMock } from '../models/menu-item-link.mock';
import { MenuStateService } from '../services/menu-state.service';

import { MainMenuComponent } from './main-menu.component';

describe('MainMenuComponent', () => {
  let component: MainMenuComponent;
  let fixture: ComponentFixture<MainMenuComponent>;

  let menuStateService: MenuStateService;
  const mockMenu = [
    new MenuItemLinkMock({ id: 'item1' }),
    new MenuItemLinkMock({ id: 'item2' }),
  ];
  mockMenu[0].appUrl = 'homeAppId';
  mockMenu[0].appPath = 'homeAppPath';
  const menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
    'isMenuItemActive',
    'activateMenuItem',
  ]);
  menuStateServiceMock.menu$ = of(mockMenu);

  let qaHelperService: QaHelperService;
  const qaHelperServiceMock = {
    getMenuItemClass: jasmine.createSpy('getMenuItemClass'),
  };

  let clientAppService: ClientAppService;
  const clientAppServiceMock = {
    connectionEstablished$: of(true),
  };

  let globalSettingsService: GlobalSettingsService;
  const globalSettingsServiceMock = {
    userSettings: {},
  };

  const bootstrappedSuccessful$ = new Subject();
  const bootstrapServiceMock = {
    bootstrappedSuccessful$,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MainMenuComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: QaHelperService, useValue: qaHelperServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: GlobalSettingsService, useValue: globalSettingsServiceMock },
        { provide: BootstrapService, useValue: bootstrapServiceMock },
      ],
    });

    fixture = TestBed.createComponent(MainMenuComponent);

    menuStateService = fixture.debugElement.injector.get(MenuStateService);
    qaHelperService = fixture.debugElement.injector.get(QaHelperService);
    clientAppService = fixture.debugElement.injector.get(ClientAppService);
    globalSettingsService = fixture.debugElement.injector.get(
      GlobalSettingsService,
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not activate until bootstrapping is finished', () => {
    expect(menuStateServiceMock.activateMenuItem).not.toHaveBeenCalled();
  });

  it('should activate the home menu item after bootstrapping', fakeAsync(() => {
    bootstrappedSuccessful$.next();

    expect(menuStateServiceMock.activateMenuItem).toHaveBeenCalledWith('homeAppId', 'homeAppPath');
  }));
});
