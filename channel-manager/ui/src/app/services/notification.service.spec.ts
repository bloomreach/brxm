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

import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';

import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let matSnackBarMock: MatSnackBar;

  beforeEach(() => {
    matSnackBarMock = {
      open: jest.fn(),
    } as unknown as typeof matSnackBarMock;

    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
      ],
      providers: [
        NotificationService,
        { provide: MatSnackBar, useValue: matSnackBarMock },
      ],
    });

    service = TestBed.inject(NotificationService);
  });

  describe('simple notification', () => {
    it('should show a snack bar with passed string message', () => {
      service.showNotification('Some message');

      expect(matSnackBarMock.open).toHaveBeenCalledWith('Some message', 'DISMISS', {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'top',
      });
    });
  });

  describe('error notification', () => {
    beforeEach(() => {
      service.showErrorNotification('Some error message');
    });

    it('should show a snack bar', () => {
      expect(matSnackBarMock.open).toHaveBeenCalledWith('Some error message', 'DISMISS', {
        horizontalPosition: 'end',
        verticalPosition: 'top',
      });
    });
  });
});
