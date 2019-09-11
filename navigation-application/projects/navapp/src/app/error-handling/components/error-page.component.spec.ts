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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { NavigationService } from '../../services/navigation.service';

import { ErrorPageComponent } from './error-page.component';

describe('ErrorPageComponent', () => {
  let component: ErrorPageComponent;
  let fixture: ComponentFixture<ErrorPageComponent>;
  let de: DebugElement;

  const navigationServiceMock = jasmine.createSpyObj('NavigationService', [
    'navigateToHome',
  ]);

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ErrorPageComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: NavigationService, useValue: navigationServiceMock },
      ],
    });

    fixture = TestBed.createComponent(ErrorPageComponent);
    de = fixture.debugElement;

    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call navigateToHome', () => {
    const goToHomeButton = de.query(By.css('.go-to-home-btn'));

    goToHomeButton.triggerEventHandler('click', {});

    expect(navigationServiceMock.navigateToHome).toHaveBeenCalled();
  });
});
