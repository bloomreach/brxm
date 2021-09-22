/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { DebugElement, NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MenuItemLinkMock } from '../../main-menu/models/menu-item-link.mock';
import { MenuStateService } from '../../main-menu/services/menu-state.service';
import { NavigationService } from '../../services/navigation.service';
import { AppError } from '../models/app-error';
import { CriticalError } from '../models/critical-error';
import { TimeoutError } from '../models/timeout-error';

import { ErrorPageComponent } from './error-page.component';

@Pipe({
  name: 'translate',
})
class TranslatePipeMock implements PipeTransform {
  name = 'translate';

  transform(query: string, ...args: any[]): any {
    return query;
  }
}

describe('ErrorPageComponent', () => {
  let component: ErrorPageComponent;
  let fixture: ComponentFixture<ErrorPageComponent>;
  let de: DebugElement;

  const navigationServiceMock = jasmine.createSpyObj('NavigationService', [
    'navigateToHome',
    'reload',
  ]);

  let menuStateServiceMock: any = {
    currentHomeMenuItem: undefined,
  };

  const translateServiceMock = {
    instant: jasmine.createSpy().and.callFake((key, params) => {
      if (params) {
        return `key: ${key}, params: ${Object.values(params).join(', ')}`;
      }

      return key;
    }),
  };

  beforeEach(() => {
    menuStateServiceMock.currentHomeMenuItem = new MenuItemLinkMock();

    TestBed.configureTestingModule({
      declarations: [ErrorPageComponent, TranslatePipeMock],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: TranslateService, useValue: translateServiceMock },
      ],
      imports: [TranslateModule.forRoot()],
    });

    fixture = TestBed.createComponent(ErrorPageComponent);
    menuStateServiceMock = TestBed.inject(MenuStateService);
    de = fixture.debugElement;

    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should hide go to home button when it is a critical error', () => {
    component.error = new CriticalError('Some critical error');

    fixture.detectChanges();

    const goToHomeButton = de.query(By.css('.go-to-home-btn'));

    expect(goToHomeButton).toBeNull();
  });

  it('should hide go to home button when there is no home menu item', () => {
    menuStateServiceMock.currentHomeMenuItem = undefined;
    component.error = new AppError(404, 'Some error');

    fixture.detectChanges();

    const goToHomeButton = de.query(By.css('.go-to-home-btn'));

    expect(goToHomeButton).toBeNull();
  });

  describe('when the error is set', () => {
    beforeEach(() => {
      component.error = new AppError(404, 'Some error');

      fixture.detectChanges();
    });

    it('should show the error message with error code', () => {
      const messageEl = de.query(By.css('.error-message'));

      expect(messageEl.nativeElement.textContent).toBe('key: ERROR_PAGE_MESSAGE, params: Some error, 404');
    });

    it('should show the go to home button', () => {
      const goToHomeButton = de.query(By.css('.go-to-home-btn'));

      expect(goToHomeButton).toBeDefined();
    });

    it('should call navigateToHome', () => {
      const goToHomeButton = de.query(By.css('.go-to-home-btn'));

      goToHomeButton.triggerEventHandler('click', {});

      expect(navigationServiceMock.navigateToHome).toHaveBeenCalled();
    });
  });

  describe('when the timeout error is set', () => {
    beforeEach(() => {
      component.error = new TimeoutError('Some error description');

      fixture.detectChanges();
    });

    it('should show the error message with error code', () => {
      const messageEl = de.query(By.css('.error-message'));

      expect(messageEl.nativeElement.textContent).toBe('key: ERROR_PAGE_MESSAGE, params: ERROR_TIMEOUT, 408');
    });

    it('should show the error description', () => {
      const messageEl = de.query(By.css('.error-description'));

      expect(messageEl.nativeElement.textContent).toBe('Some error description');
    });

    it('should hide the go to home button', () => {
      const goToHomeButton = de.query(By.css('.go-to-home-btn'));

      expect(goToHomeButton).toBeNull();
    });

    it('should show the reload button', () => {
      const reloadPageButton = de.query(By.css('.reload-page-btn'));

      expect(reloadPageButton).toBeDefined();
    });

    it('should call reload', () => {
      const reloadPageButton = de.query(By.css('.reload-page-btn'));

      reloadPageButton.triggerEventHandler('click', {});

      expect(navigationServiceMock.reload).toHaveBeenCalled();
    });
  });
});
