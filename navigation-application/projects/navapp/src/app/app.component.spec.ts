/*
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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { AppComponent } from './app.component';
import { APP_BOOTSTRAPPED } from './bootstrap/app-bootstrapped';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { AppSettingsMock } from './models/dto/app-settings.mock';
import { UserSettingsMock } from './models/dto/user-settings.mock';
import { APP_SETTINGS } from './services/app-settings';
import { OverlayService } from './services/overlay.service';
import { PENDO } from './services/pendo';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let de: DebugElement;

  let bootstrappedResolve: () => void;
  let bootstrappedReject: () => void;

  let rightSidePanelService: jasmine.SpyObj<RightSidePanelService>;
  let pendo: jasmine.SpyObj<pendo.Pendo>;
  const userSettings = new UserSettingsMock();
  const appSettings = new AppSettingsMock();

  beforeEach(() => {
    const translateServiceMock = jasmine.createSpyObj('TranslateService', [
      'addLangs',
      'setDefaultLang',
      'use',
    ]);

    const overlayServiceMock = {
      visible$: of(false),
    };

    const rightSidePanelServiceMock = jasmine.createSpyObj('RightSidePanelService', [
      'setSidenav',
    ]);

    const errorHandlingServiceMock = {
      currentError: {},
    };

    const pendoMock = jasmine.createSpyObj<pendo.Pendo>('PENDO', ['initialize', 'enableDebugging']);

    const bootstrappedMock = new Promise<void>((res, rej) => {
      bootstrappedResolve = res;
      bootstrappedReject = rej;
    });

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
      ],
      declarations: [AppComponent],
      providers: [
        { provide: TranslateService, useValue: translateServiceMock },
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: RightSidePanelService, useValue: rightSidePanelServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
        { provide: APP_BOOTSTRAPPED, useValue: bootstrappedMock },
        { provide: PENDO, useValue: pendoMock },
        { provide: APP_SETTINGS, useValue: appSettings },
        { provide: USER_SETTINGS, useValue: userSettings },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(AppComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    rightSidePanelService = TestBed.get(RightSidePanelService);
    pendo = TestBed.get(PENDO);

    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeDefined();
  });

  describe('upon initialization', () => {
    beforeEach(() => {
      component.sidenav = {} as any;
    });

    it('should set the side nav DOM element', () => {
      component.ngOnInit();

      expect(rightSidePanelService.setSidenav).toHaveBeenCalledWith(component.sidenav);
    });

    it('should initialize pendo and track visitor by email', () => {
      const testEmail = 'asdf@gmail.com';

      Object.assign(userSettings, new UserSettingsMock({
        email: testEmail,
      }));
      const expectedConfig = {
        visitor: {
          id: testEmail,
        },
      };

      component.ngOnInit();

      expect(pendo.initialize).toHaveBeenCalledWith(expectedConfig);
    });

    it('should initialize pendo and fall back to track visitor by username', () => {
      const testName = 'testuser';
      Object.assign(userSettings, new UserSettingsMock({
        email: null,
        userName: testName,
      }));
      const expectedConfig = {
        visitor: {
          id: testName,
        },
      };

      component.ngOnInit();

      expect(pendo.initialize).toHaveBeenCalledWith(expectedConfig);
    });
  });

  describe('loading indicator', () => {
    it('should be shown until the app is bootstrapped', () => {
      const loader = de.query(By.css('brna-loader'));

      expect(loader).not.toBeNull();
    });

    it('should be hidden after the app is bootstrapped', fakeAsync(() => {
      bootstrappedResolve();

      tick();

      fixture.detectChanges();

      tick();

      const loader = de.query(By.css('brna-loader'));

      expect(loader).toBeNull();
    }));

    it('should be hidden  after the app\'s bootstrapping is failed', fakeAsync(() => {
      bootstrappedReject();

      tick();

      fixture.detectChanges();

      tick();

      const loader = de.query(By.css('brna-loader'));

      expect(loader).toBeNull();
    }));
  });
});
