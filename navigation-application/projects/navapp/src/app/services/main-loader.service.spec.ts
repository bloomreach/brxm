/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { APP_BOOTSTRAPPED } from '../bootstrap/app-bootstrapped';

import { MainLoaderService } from './main-loader.service';

describe('MainLoaderService', () => {
  let service: MainLoaderService;
  let resolveAppBootstrappedPromise: () => void;
  let rejectAppBootstrappedPromise: () => void;

  beforeEach(() => {
    const appBootstrappedPromise = new Promise((resolve, reject) => {
      resolveAppBootstrappedPromise = resolve;
      rejectAppBootstrappedPromise = reject;
    });

    TestBed.configureTestingModule({
      providers: [
        { provide: APP_BOOTSTRAPPED, useValue: appBootstrappedPromise },
        MainLoaderService,
      ],
    });

    service = TestBed.get(MainLoaderService);
  });

  it('should be visible initially', () => {
    expect(service.isVisible).toBeTruthy();
  });

  it('should be hidden when appBootstrap promise is resolved', fakeAsync(() => {
    resolveAppBootstrappedPromise();

    tick();

    expect(service.isVisible).toBeFalsy();
  }));

  it('should be hidden when appBootstrap promise is rejected', fakeAsync(() => {
    rejectAppBootstrappedPromise();

    tick();

    expect(service.isVisible).toBeFalsy();
  }));

  it('should not hide the loader', () => {
    service.hide();

    expect(service.isVisible).toBeTruthy();
  });

  describe('after appBootstrap promise is resolved', () => {
    beforeEach(async(() => {
      resolveAppBootstrappedPromise();
    }));

    it('should show the loader', () => {
      service.show();

      expect(service.isVisible).toBeTruthy();
    });

    describe('if the loader is visible', () => {
      it('should hide it', () => {
        service.hide();

        expect(service.isVisible).toBeFalsy();
      });
    });
  });
});
