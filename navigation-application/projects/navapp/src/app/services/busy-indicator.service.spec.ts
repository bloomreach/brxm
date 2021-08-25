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

import { TestBed } from '@angular/core/testing';

import { BusyIndicatorService } from './busy-indicator.service';

describe('BusyIndicatorService', () => {
  let busyIndicatorService: BusyIndicatorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BusyIndicatorService,
      ],
    });

    busyIndicatorService = TestBed.inject(BusyIndicatorService);
  });

  it('busy indicator should be hidden by default', () => {
    expect(busyIndicatorService.isVisible).toBeFalsy();
  });

  it('should show the busy indicator', () => {
    busyIndicatorService.show();

    expect(busyIndicatorService.isVisible).toBeTruthy();
  });

  it('should hide the busy indicator', () => {
    busyIndicatorService.hide();

    expect(busyIndicatorService.isVisible).toBeFalsy();
  });
});
