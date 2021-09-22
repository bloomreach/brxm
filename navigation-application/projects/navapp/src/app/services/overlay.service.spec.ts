/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { Subject } from 'rxjs';

import { ConnectionService } from './connection.service';
import { OverlayService } from './overlay.service';

describe('OverlayService', () => {
  let service: OverlayService;

  const showMask = new Subject<void>();
  const hideMask = new Subject<void>();

  beforeEach(() => {
    const connectionServiceMock = {
      showMask$: showMask,
      hideMask$: hideMask,
    };
    TestBed.configureTestingModule({
      providers: [
        OverlayService,
        { provide: ConnectionService, useValue: connectionServiceMock },
      ],
    });

    service = TestBed.inject(OverlayService);
  });

  it('should be hidden by default', () => {
    expect(service.isVisible).toBeFalsy();
  });

  it('should show it', () => {
    showMask.next();

    expect(service.isVisible).toBeTruthy();
  });

  describe('when it is visible', () => {
    beforeEach(() => {
      showMask.next();
    });

    it('should hide it', () => {
      hideMask.next();

      expect(service.isVisible).toBeFalsy();
    });
  });

  it('should be hidden after multiple visibility switches', () => {
    showMask.next();
    hideMask.next();
    showMask.next();
    hideMask.next();

    expect(service.isVisible).toBeFalsy();
  });

  it('should be visible if hideMask is called less times as showMask', () => {
    showMask.next();
    showMask.next();
    showMask.next();
    hideMask.next();
    hideMask.next();

    expect(service.isVisible).toBeTruthy();
  });

  it('should be hidden if hideMask is called the same number of times as showMask', () => {
    showMask.next();
    showMask.next();
    showMask.next();
    hideMask.next();
    hideMask.next();
    hideMask.next();

    expect(service.isVisible).toBeFalsy();
  });

  it('should be visible if hideMask is called less times than showMask after first showMask call', () => {
    hideMask.next();
    showMask.next();
    showMask.next();
    hideMask.next();
    showMask.next();

    expect(service.isVisible).toBeTruthy();
  });
});
