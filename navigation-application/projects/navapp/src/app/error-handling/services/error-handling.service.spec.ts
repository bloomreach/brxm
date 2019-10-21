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

import { TestBed } from '@angular/core/testing';
import { ClientErrorCodes } from '@bloomreach/navapp-communication';
import { of } from 'rxjs';

import { ConnectionService } from '../../services/connection.service';
import { AppError } from '../models/app-error';
import { CriticalError } from '../models/critical-error';
import { InternalError } from '../models/internal-error';
import { NotFoundError } from '../models/not-found-error';

import { ErrorHandlingService } from './error-handling.service';

describe('ErrorHandlingService', () => {
  let errorHandlingService: ErrorHandlingService;

  beforeEach(() => {
    spyOn(console, 'error');

    const connectionServiceMock = {
      onError$: of(),
    };

    TestBed.configureTestingModule({
      providers: [
        ErrorHandlingService,
        { provide: ConnectionService, useValue: connectionServiceMock },
      ],
    });

    errorHandlingService = TestBed.get(ErrorHandlingService);
  });

  it('should set the AppError', () => {
    const expected = new AppError(
      500,
      'Some title',
      'Some detailed message',
    );

    errorHandlingService.setError(expected);

    expect(errorHandlingService.currentError).toBe(expected);
  });

  it('should set the AppError basing on a client error', () => {
    const expected = new AppError(
      500,
      'Something went wrong',
      'Optional message',
    );

    errorHandlingService.setClientError(ClientErrorCodes.UnknownError, 'Optional message');

    expect(errorHandlingService.currentError).toEqual(expected);
  });

  it('should set the CriticalError', () => {
    const expected = new CriticalError('Some critical error', 'Description for logs');

    errorHandlingService.setCriticalError('Some critical error', 'Description for logs');

    expect(errorHandlingService.currentError).toEqual(expected);
  });

  it('should set the NotFoundError', () => {
    const expected = new NotFoundError('Some available to the user description', 'Description for logs');

    errorHandlingService.setNotFoundError('Some available to the user description', 'Description for logs');

    expect(errorHandlingService.currentError).toEqual(expected);
  });

  it('should set the InternalError', () => {
    const expected = new InternalError('Some available to the user description', 'Description for logs');

    errorHandlingService.setInternalError('Some available to the user description', 'Description for logs');

    expect(errorHandlingService.currentError).toEqual(expected);
  });

  describe('when the error is set', () => {
    beforeEach(() => {
      const expected = new AppError(
        500,
        'Some title',
        'Some detailed message',
      );

      errorHandlingService.setError(expected);
    });

    it('should clean the error', () => {
      errorHandlingService.clearError();

      expect(errorHandlingService.currentError).toBeUndefined();
    });
  });
});
