/*!
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

import { fakeAsync, tick } from '@angular/core/testing';

import { CriticalError } from '../error-handling/models/critical-error';

import { appBootstrappedPromise, scheduleAppBootstrapping } from './schedule-app-bootstrapping';

describe('scheduleAppBootstrapping', () => {
  const bootstrapServiceMock = jasmine.createSpyObj('BootstrapService', [
    'bootstrap',
  ]);

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  const errorHandlingServiceMock = jasmine.createSpyObj('ErrorHandlingService', [
    'setError',
    'setInternalError',
  ]);

  beforeEach(() => {
    bootstrapServiceMock.bootstrap.and.returnValue(Promise.resolve());
  });

  it('should show busy indicator', () => {
    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

    expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
  });

  it('should call bootstrap() method', () => {
    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

    expect(bootstrapServiceMock.bootstrap).toHaveBeenCalled();
  });

  it('should hide busy indicator when bootstrap promise was resolved', () => {
    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

    expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
  });

  it('should hide busy indicator when bootstrap promise was rejected', () => {
    bootstrapServiceMock.bootstrap.and.returnValue(Promise.reject());

    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

    expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
  });

  it('should resolve appBootstrappedPromise after successful bootstrapping', fakeAsync(() => {
    let resolved = false;

    appBootstrappedPromise.then(() => resolved = true);

    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

    tick();

    expect(resolved).toBeTruthy();
  }));

  describe('in case of the error', () => {
    it('set the error if the rejection reason is a subtype of AppError', fakeAsync(() => {
      const expectedError = new CriticalError('Something went wrong', 'Some internal description');
      bootstrapServiceMock.bootstrap.and.returnValue(Promise.reject(expectedError));

      scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

      tick();

      expect(errorHandlingServiceMock.setError).toHaveBeenCalledWith(expectedError);
    }));

    it('set the internal error if the rejection reason is an Error', fakeAsync(() => {
      const expectedError = new Error('Some error');
      bootstrapServiceMock.bootstrap.and.returnValue(Promise.reject(expectedError));

      scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

      tick();

      expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
        'An error occurred during initialization',
        'Error: Some error',
      );
    }));

    it('set the internal error if the rejection reason is a string', fakeAsync(() => {
      const expected = 'Some error';
      bootstrapServiceMock.bootstrap.and.returnValue(Promise.reject(expected));

      scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

      tick();

      expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
        'An error occurred during initialization',
        expected,
      );
    }));

    it('set the internal error if the rejection reason is undefined', fakeAsync(() => {
      const expected = undefined;
      bootstrapServiceMock.bootstrap.and.returnValue(Promise.reject(expected));

      scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock, errorHandlingServiceMock);

      tick();

      expect(errorHandlingServiceMock.setInternalError).toHaveBeenCalledWith(
        'An error occurred during initialization',
        expected,
      );
    }));
  });
});
