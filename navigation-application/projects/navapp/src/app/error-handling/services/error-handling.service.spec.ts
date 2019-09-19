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

import { ClientErrorCodes } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AppError } from '../models/appError';

import { ErrorHandlingService } from './error-handling.service';

describe('ErrorHandlingService', () => {
  let service: ErrorHandlingService;

  beforeEach(() => {
    service = new ErrorHandlingService();
  });

  it('should set the AppError', () => {
    const expected = new AppError(
      500,
      'Some title',
      'Some detailed message',
    );

    service.setError(expected);

    expect(service.currentError).toBe(expected);
  });

  it('should set the AppError basing on a client error', () => {
    const expected = new AppError(
      500,
      'Something went wrong',
      'Optional message',
    );

    service.setClientError(ClientErrorCodes.UnknownError, 'Optional message');

    expect(service.currentError).toEqual(expected);
  });

  describe('when the error is set', () => {
    beforeEach(() => {
      const expected = new AppError(
        500,
        'Some title',
        'Some detailed message',
      );

      service.setError(expected);
    });

    it('should clean the error', () => {
      service.clearError();

      expect(service.currentError).toBeUndefined();
    });
  });
});
