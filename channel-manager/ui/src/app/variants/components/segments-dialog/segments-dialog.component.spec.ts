/*
 * Copyright 2020-2023 Bloomreach
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
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { TranslateModule } from '@ngx-translate/core';

import { TargetingApiResponse } from '../../../models/targeting-api-response.model';
import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../../services/ng1/targeting.ng1.service';
import { Persona } from '../../models/persona.model';

import { SegmentsDialogComponent } from './segments-dialog.component';

describe('SegmentsDialogComponent', () => {
  let component: SegmentsDialogComponent;
  let fixture: ComponentFixture<SegmentsDialogComponent>;

  let targetingService: Ng1TargetingService;

  const personas: Partial<Persona>[] = [
    { segmentName: 'Walmart' },
    { segmentName: 'Cosco' },
    { segmentName: 'Vons' },
    { segmentName: '7eleven' },
  ];

  const mockApiResponse: TargetingApiResponse<{ items: Persona[] }> = {
    success: true,
    message: 'Some message',
    errorCode: null,
    reloadRequired: false,
    data: {
      items: personas as Persona[],
    },
  };

  beforeEach(() => {
    const targetingServiceMock = {
      getPersonas: jest.fn(() => Promise.resolve(mockApiResponse)),
    };

    const matDialogRefMock = {};

    TestBed.configureTestingModule({
      imports: [
        MatDialogModule,
        MatListModule,
        TranslateModule.forRoot(),
      ],
      declarations: [SegmentsDialogComponent],
      providers: [
        { provide: NG1_TARGETING_SERVICE, useValue: targetingServiceMock },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });

    targetingService = TestBed.inject(NG1_TARGETING_SERVICE);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SegmentsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('segments list', () => {
    it('should render personas list alphabetically', async () => {
      await component.ngOnInit();
      fixture.detectChanges();

      const segmentListElement = fixture.nativeElement.querySelector('.qa-segments');
      expect(segmentListElement).toMatchSnapshot();
    });
  });
});
