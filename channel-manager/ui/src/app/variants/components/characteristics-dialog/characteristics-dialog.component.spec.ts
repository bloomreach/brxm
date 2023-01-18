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

import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../../../services/ng1/config.ng1.service';
import { NG1_TARGETING_SERVICE } from '../../../services/ng1/targeting.ng1.service';

import { CharacteristicsDialogComponent } from './characteristics-dialog.component';

describe('CharacteristicsDialogComponent', () => {

  @Component({
    // tslint:disable-next-line:component-selector
    selector: 'mat-icon',
    template: '{{ svgIcon }}',
  })
  class MatIconMockComponent {
    @Input()
    svgIcon!: string;
  }

  const configServiceMock: Partial<Ng1ConfigService> = {};

  let component: CharacteristicsDialogComponent;
  let fixture: ComponentFixture<CharacteristicsDialogComponent>;

  beforeEach(() => {
    const targetingServiceMock = {};
    const matDialogRefMock = {};

    TestBed.configureTestingModule({
      imports: [
        MatDialogModule,
        MatListModule,
        MatTooltipModule,
        TranslateModule.forRoot(),
      ],
      declarations: [
        CharacteristicsDialogComponent,
        MatIconMockComponent,
      ],
      providers: [
        { provide: NG1_CONFIG_SERVICE, useValue: configServiceMock },
        { provide: NG1_TARGETING_SERVICE, useValue: targetingServiceMock },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CharacteristicsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
