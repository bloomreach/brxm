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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { ClientAppService } from '../../../client-app/services/client-app.service';
import { AppSettingsMock } from '../../../models/dto/app-settings.mock';
import { UserSettingsMock } from '../../../models/dto/user-settings.mock';
import { APP_SETTINGS } from '../../../services/app-settings';
import { AuthService } from '../../../services/auth.service';
import { USER_SETTINGS } from '../../../services/user-settings';

import { UserToolbarDrawerComponent } from './user-toolbar-drawer.component';

describe('User Drawer Component', () => {
  let component: UserToolbarDrawerComponent;
  let fixture: ComponentFixture<UserToolbarDrawerComponent>;
  let logoutDe: DebugElement;

  let authService: jasmine.SpyObj<AuthService>;
  const userName = 'myUserName';
  const userEmail = 'userEmail';

  beforeEach(async(() => {
    const clientAppServiceMock = {
      allConnectionsSettled: Promise.resolve(),
    };
    const authServiceMock = jasmine.createSpyObj('AuthService', ['activeLogout']);
    const appSettingsMock = new AppSettingsMock({
      navAppBaseURL: 'https://some-domain.com/base/path',
    });
    const userSettingsMock = new UserSettingsMock({
      userName,
      email: userEmail,
    });

    fixture = TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      declarations: [UserToolbarDrawerComponent],
      providers: [
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: USER_SETTINGS, useValue: userSettingsMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(UserToolbarDrawerComponent);

    authService = TestBed.get(AuthService);

    component = fixture.componentInstance;

    logoutDe = fixture.debugElement.query(By.css('.qa-usersettings-logout'));

    fixture.detectChanges();
  }));

  it('should exist', () => {
    expect(component).toBeTruthy();
  });

  it('should logout', () => {
    logoutDe.triggerEventHandler('click', { preventDefault: () => {} });

    expect(authService.activeLogout).toHaveBeenCalled();
  });

  it('should prevent default action if user clicked "logout"', () => {
    const eventMock = jasmine.createSpyObj('ClickEvent', [
      'preventDefault',
    ]);

    logoutDe.triggerEventHandler('click', eventMock);

    expect(eventMock.preventDefault).toHaveBeenCalled();
  });

  it('should return the userName', () => {
    const name = component.userName;
    expect(name).toEqual(userName);
  });

  it('should return the user email or empty string', () => {
    const email = component.email;
    expect(email).toEqual(userEmail);
  });

  it('should emit userDrawerOpenChange if user clicked outside', () => {
    spyOn(component.userDrawerOpenChange, 'emit');

    component.onClickedOutside();
    expect(component.userDrawerOpenChange.emit).toHaveBeenCalledWith(false);
  });

  it('should emit userDrawerOpenChange if user clicked "logout"', () => {
    spyOn(component.userDrawerOpenChange, 'emit');

    component.onClickedOutside();
    expect(component.userDrawerOpenChange.emit).toHaveBeenCalledWith(false);
  });
});
