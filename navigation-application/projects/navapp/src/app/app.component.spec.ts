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
import { of } from 'rxjs';

import { AppComponent } from './app.component';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { OverlayService } from './services/overlay.service';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  const overlayServiceMock = {
    visible$: of(false),
  };

  const rightSidePanelServiceMock = jasmine.createSpyObj('RightSidePanelService', [
    'setSideNav',
  ]);

  const errorHandlingServiceMock = {
    currentError: {},
  };

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [AppComponent],
      providers: [
        { provide: OverlayService, useValue: overlayServiceMock },
        { provide: RightSidePanelService, useValue: rightSidePanelServiceMock },
        { provide: ErrorHandlingService, useValue: errorHandlingServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(AppComponent);

    component = fixture.componentInstance;
  });

  it('should create the app', () => {
    expect(component).toBeDefined();
  });
});
