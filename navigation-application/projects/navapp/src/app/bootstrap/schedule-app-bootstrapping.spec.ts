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

import { appBootstrappedPromise, scheduleAppBootstrapping } from './schedule-app-bootstrapping';

describe('scheduleAppBootstrapping', () => {
  const bootstrapServiceMock = jasmine.createSpyObj('BootstrapService', [
    'bootstrap',
  ]);

  const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
    'show',
    'hide',
  ]);

  beforeEach(() => {
    bootstrapServiceMock.bootstrap.and.returnValue(Promise.resolve());
  });

  it('should show busy indicator', () => {
    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock);

    expect(busyIndicatorServiceMock.show).toHaveBeenCalled();
  });

  it('should call bootstrap() method', () => {
    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock);

    expect(bootstrapServiceMock.bootstrap).toHaveBeenCalled();
  });

  it('should hide busy indicator when bootstrap promise was resolved', () => {
    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock);

    expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
  });

  it('should hide busy indicator when bootstrap promise was rejected', () => {
    bootstrapServiceMock.bootstrap.and.returnValue(Promise.reject());

    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock);

    expect(busyIndicatorServiceMock.hide).toHaveBeenCalled();
  });

  it('should resolve appBootstrappedPromise after successful bootstrapping', fakeAsync(() => {
    let resolved = false;

    appBootstrappedPromise.then(() => resolved = true);

    scheduleAppBootstrapping(bootstrapServiceMock, busyIndicatorServiceMock);

    tick();

    expect(resolved).toBeTruthy();
  }));
});
