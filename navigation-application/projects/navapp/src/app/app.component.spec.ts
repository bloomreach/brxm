/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AppComponent } from './app.component';
import { AppError } from './error-handling/models/app-error';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { AppSettings } from './models/dto/app-settings.dto';
import { AppSettingsMock } from './models/dto/app-settings.mock';
import { UserSettingsMock } from './models/dto/user-settings.mock';
import { APP_SETTINGS } from './services/app-settings';
import { MainLoaderService } from './services/main-loader.service';
import { OverlayService } from './services/overlay.service';
import { PageTitleManagerService } from './services/page-title-manager.service';
import { PENDO } from './services/pendo';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let de: DebugElement;

  let overlayServiceMock: OverlayService;
  let rightSidePanelServiceMock: jasmine.SpyObj<RightSidePanelService>;
  let errorHandlingServiceMock: ErrorHandlingService;
  let mainLoaderServiceMock: MainLoaderService;
  let pageTitleManagerServiceMock: jasmine.SpyObj<PageTitleManagerService>;
  let pendoMock: jasmine.SpyObj<pendo.Pendo>;
  const userSettings = new UserSettingsMock();
  let appSettings: AppSettings;

  beforeEach(() => {
    overlayServiceMock = {
      isVisible: false,
    } as any;

    rightSidePanelServiceMock = jasmine.createSpyObj('RightSidePanelService', [
      'setSidenav',
    ]);

    errorHandlingServiceMock = {
      currentError: undefined,
    } as any;

    mainLoaderServiceMock = {
      isVisible: false,
    } as any;

    pageTitleManagerServiceMock = jasmine.createSpyObj('PageTitleManagerService', [
      'init',
    ]);

    pendoMock = jasmine.createSpyObj<pendo.Pendo>('PENDO', ['initialize', 'enableDebugging']);

    appSettings = new AppSettingsMock();

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
      ],
      declarations: [AppComponent],
      providers: [
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: RightSidePanelService, useValue: rightSidePanelServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
        { provide: MainLoaderService, useValue: mainLoaderServiceMock },
        { provide: PageTitleManagerService, useValue: pageTitleManagerServiceMock },
        { provide: PENDO, useValue: pendoMock },
        { provide: APP_SETTINGS, useValue: appSettings },
        { provide: USER_SETTINGS, useValue: userSettings },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(AppComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    overlayServiceMock = TestBed.get(OverlayService);
    rightSidePanelServiceMock = TestBed.get(RightSidePanelService);
    errorHandlingServiceMock = TestBed.get(ErrorHandlingService);
    mainLoaderServiceMock = TestBed.get(MainLoaderService);
    appSettings = TestBed.get(APP_SETTINGS);
  });

  it('should create the app', () => {
    expect(component).toBeDefined();
  });

  it('should return the current error', () => {
    const expected = new AppError(500, 'some error');

    (errorHandlingServiceMock as any).currentError = expected;

    const actual = component.error;

    expect(actual).toBe(expected);
  });

  it('should return overlay\'s visibility state', () => {
    (overlayServiceMock as any).isVisible = true;

    const actual = component.isOverlayVisible;

    expect(actual).toBeTruthy();
  });

  it('should return main loader\'s visibility state', () => {
    (mainLoaderServiceMock as any).isVisible = true;

    const actual = component.isLoaderVisible;

    expect(actual).toBeTruthy();
  });

  describe('upon initialization', () => {
    beforeEach(() => {
      component.sidenav = {} as any;
    });

    it('should set the side nav DOM element', () => {
      component.ngOnInit();

      expect(rightSidePanelServiceMock.setSidenav).toHaveBeenCalledWith(component.sidenav);
    });

    it('should initialize PageTitleManagerService', () => {
      component.ngOnInit();

      expect(pageTitleManagerServiceMock.init).toHaveBeenCalled();
    });

    it('should initialize pendo and track visitor by email and accountId', () => {
      const testEmail = 'asdf@gmail.com';

      Object.assign(userSettings, new UserSettingsMock({
        email: testEmail,
      }));
      const expectedConfig = {
        visitor: {
          id: testEmail,
        },
        account: {
          id: 'testAccount',
        },
      };

      component.ngOnInit();

      expect(pendoMock.initialize).toHaveBeenCalledWith(expectedConfig);
    });

    it('should initialize pendo and fall back to track visitor by username and accountId', () => {
      const testName = 'testuser';

      Object.assign(userSettings, new UserSettingsMock({
        email: null,
        userName: testName,
      }));
      const expectedConfig = {
        visitor: {
          id: testName,
        },
        account: {
          id: 'testAccount',
        },
      };

      component.ngOnInit();

      expect(pendoMock.initialize).toHaveBeenCalledWith(expectedConfig);
    });

    it('should not initialize pendo if usage statistics collection is disabled', () => {
      appSettings.usageStatisticsEnabled = false;

      component.ngOnInit();

      expect(pendoMock.initialize).not.toHaveBeenCalled();
    });
  });
});
