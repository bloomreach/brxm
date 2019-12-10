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
import { UserSettings } from './models/dto/user-settings.dto';
import { UserSettingsMock } from './models/dto/user-settings.mock';
import { OverlayService } from './services/overlay.service';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let de: DebugElement;

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

  let userSettingsMock: UserSettings;

  let bootstrappedResolve: () => void;
  let bootstrappedReject: () => void;

  beforeEach(() => {
    userSettingsMock = new UserSettingsMock();

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
        { provide: USER_SETTINGS, useValue: userSettingsMock },
        { provide: APP_BOOTSTRAPPED, useValue: bootstrappedMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(AppComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeDefined();
  });

  describe('upon initialization', () => {
    beforeEach(() => {
      component.sidenav = {} as any;

      component.ngOnInit();
    });

    it('should set the side nav DOM element', () => {
      expect(rightSidePanelServiceMock.setSidenav).toHaveBeenCalledWith(component.sidenav);
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
