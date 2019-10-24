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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { AppComponent } from './app.component';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { UserSettings } from './models/dto/user-settings.dto';
import { UserSettingsMock } from './models/dto/user-settings.mock';
import { OverlayService } from './services/overlay.service';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

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

  beforeEach(() => {
    userSettingsMock = new UserSettingsMock();

    fixture = TestBed.configureTestingModule({
      declarations: [AppComponent],
      providers: [
        { provide: TranslateService, useValue: translateServiceMock },
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: RightSidePanelService, useValue: rightSidePanelServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
        { provide: USER_SETTINGS, useValue: userSettingsMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(AppComponent);

    component = fixture.componentInstance;
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

  describe('translate service set up', () => {
    it('should add languages', () => {
      const expectedLanguages = [
        'en',
        'nl',
        'fr',
        'de',
        'es',
        'zh',
      ];

      component.ngOnInit();

      expect(translateServiceMock.addLangs).toHaveBeenCalledWith(expectedLanguages);
    });

    it('should set the default language', () => {
      const expected = 'en';

      component.ngOnInit();

      expect(translateServiceMock.setDefaultLang).toHaveBeenCalledWith(expected);
    });

    it('should use the language from user settings', () => {
      const expected = 'en';

      component.ngOnInit();

      expect(translateServiceMock.use).toHaveBeenCalledWith(expected);
    });

    it('should use the default language if user settings do not contain the language setting', () => {
      const expected = 'en';

      userSettingsMock.language = undefined;
      component.ngOnInit();

      expect(translateServiceMock.use).toHaveBeenCalledWith(expected);
    });
  });
});
