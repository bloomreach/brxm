/*
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { TestBed } from '@angular/core/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule, MatSnackBarRef, MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';

import { SnackBarComponent } from './snack-bar.component';

describe('SnackBarComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatIconModule,
        MatSnackBarModule,
      ],
      declarations: [
        SnackBarComponent,
      ],
      providers: [
        { provide: MatSnackBarRef, useValue: MatSnackBarRef },
        { provide: MAT_SNACK_BAR_DATA, useValue: MAT_SNACK_BAR_DATA },
      ],
    }).compileComponents();
  });

  test('should create the snackbar', () => {
    const fixture = TestBed.createComponent(SnackBarComponent);
    const component = fixture.componentInstance;

    expect(component).toBeTruthy();
  });
});
