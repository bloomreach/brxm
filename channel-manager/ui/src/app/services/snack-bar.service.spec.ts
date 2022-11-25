/*!
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { TestBed } from '@angular/core/testing';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { SnackBarComponent } from '../shared/components/snackbar/snack-bar.component';

import { SnackBarService } from './snack-bar.service';

describe('SnackBarService', () => {
  let snackBarService: SnackBarService;

  const snackBarMock = {
    openFromComponent: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        MatSnackBarModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    });

    snackBarService = TestBed.inject(SnackBarService);
  });

  test('should be created', () => {
    expect(snackBarService).toBeTruthy();
  });

  test('should open snackbar', () => {
    const config = {
      data: {
        dismiss: undefined,
        message: 'Some message',
        warning: undefined,
      },
      duration: 4000,
    };

    snackBarService.open(config.data.message);

    expect(snackBarMock.openFromComponent).toHaveBeenCalledWith(SnackBarComponent, config);
  });

  test('should be possible to change snackbar configuration', () => {
    const config = {
      data: {
        dismiss: true,
        message: 'Some message',
        warning: true,
      },
      duration: 1000,
    };

    snackBarService.open('Some message', {
      ...config.data,
      duration: config.duration,
    });

    expect(snackBarMock.openFromComponent).toHaveBeenCalledWith(SnackBarComponent, config);
  });

  test('should show warning snackbar', () => {
    const config = {
      data: {
        message: 'Warning message',
        dismiss: true,
        warning: true,
      },
      duration: undefined,
    };

    snackBarService.warning(config.data.message);

    expect(snackBarMock.openFromComponent).toHaveBeenCalledWith(SnackBarComponent, config);
  });
});
