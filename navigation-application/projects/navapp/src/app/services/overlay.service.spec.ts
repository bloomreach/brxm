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

import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { BehaviorSubject, of, Subject } from 'rxjs';

import { ConnectionService } from './connection.service';
import { OverlayService } from './overlay.service';

describe('OverlayService', () => {
  let overlayService: OverlayService;
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

    overlayService = TestBed.get(OverlayService);
  });

  it('should save state for overlay visibility', () => {
    overlayService.visible$.subscribe(visible => {
      expect(visible).toBe(false);
    }).unsubscribe();
  });

  it('should set visibility', () => {
    showMask.next();
    overlayService.visible$
      .subscribe(visible => {
        expect(visible).toBe(true);
      })
      .unsubscribe();
    hideMask.next();
    showMask.next();
    hideMask.next();
    overlayService.visible$
      .subscribe(visible => {
        expect(visible).toBe(false);
      })
      .unsubscribe();
  });

  it('should return invisible if hideMask is called the same number of times as showMask', () => {

    const actualVisible = [];
    const subscription = overlayService.visible$
      .subscribe(visible => actualVisible.push(visible));

    showMask.next();
    showMask.next();
    showMask.next();
    hideMask.next();
    hideMask.next();
    hideMask.next();
    subscription.unsubscribe();

    const expectedVisible = [false, true, true, true, true, true, false];
    expect(actualVisible).toEqual(expectedVisible);
  });

});
