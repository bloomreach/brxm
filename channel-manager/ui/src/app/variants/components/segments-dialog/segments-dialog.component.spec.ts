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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { TranslateModule } from '@ngx-translate/core';

import { NG1_TARGETING_SERVICE } from '../../../services/ng1/targeting.ng1service';

import { SegmentsDialogComponent } from './segments-dialog.component';

describe('SegmentsDialogComponent', () => {
  let component: SegmentsDialogComponent;
  let fixture: ComponentFixture<SegmentsDialogComponent>;

  beforeEach(() => {
    const targetingServiceMock = {};
    const matDialogDataMock = {};
    const matDialogRefMock = {};

    TestBed.configureTestingModule({
      imports: [
        MatDialogModule,
        MatListModule,
        TranslateModule.forRoot(),
      ],
      declarations: [ SegmentsDialogComponent ],
      providers: [
        { provide: NG1_TARGETING_SERVICE, useValue: targetingServiceMock },
        { provide: MatDialogRef, useValue: matDialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: matDialogDataMock },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SegmentsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
