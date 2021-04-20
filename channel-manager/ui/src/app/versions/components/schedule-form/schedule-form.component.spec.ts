/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { NgxMatDatetimePickerModule, NGX_MAT_DATE_FORMATS } from '@angular-material-components/datetime-picker';
import { NgxMomentDateModule, NGX_MAT_MOMENT_DATE_ADAPTER_OPTIONS } from '@angular-material-components/moment-adapter';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { NG1_UI_ROUTER_GLOBALS } from '../../../services/ng1/ui-router-globals.ng1.service';
import { Version } from '../../models/version.model';
import { VersionsService } from '../../services/versions.service';

import { ScheduleFormComponent } from './schedule-form.component';

describe('ScheduleFormComponent', () => {
  let component: ScheduleFormComponent;
  let fixture: ComponentFixture<ScheduleFormComponent>;

  const firstVersionUUID = 'testId';
  const date = Date.parse('11/08/2020 16:03');
  const mockVersion = {
    label: 'versionName',
    jcrUUID: firstVersionUUID,
    branchId: 'master',
    userName: 'testUserName',
    timestamp: date,
  } as Version;

  beforeEach(waitForAsync(() => {
    const uiRouterGlobalsMock = {
      params: {
        documentId: 'testDocumentId',
      },
    };

    const versionsServiceMock = {
      updateVersion: jest.fn(),
      getVersionsInfo: () => Promise.resolve(),
    };

    TestBed.configureTestingModule({
      declarations: [ ScheduleFormComponent ],
      imports: [
        MatFormFieldModule,
        MatInputModule,
        MatDatepickerModule,
        NgxMatDatetimePickerModule,
        NgxMomentDateModule,
        BrowserAnimationsModule,
        ReactiveFormsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: NGX_MAT_DATE_FORMATS,
          useValue: {
            parse: {
              dateInput: ['LT'],
            },
            display: {
              dateInput: 'LLL',
              dateA11yLabel: 'LLL',
              monthYearLabel: 'MMM YYYY',
              monthYearA11yLabel: 'MMMM YYYY',
            },
          },
        },
        {
          provide: NGX_MAT_MOMENT_DATE_ADAPTER_OPTIONS,
          useValue: { strict: true, useUtc: true },
        },
        { provide: NG1_UI_ROUTER_GLOBALS, useValue: uiRouterGlobalsMock },
        { provide: VersionsService, useValue: versionsServiceMock },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ScheduleFormComponent);
    component = fixture.componentInstance;
    component.version = mockVersion;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
