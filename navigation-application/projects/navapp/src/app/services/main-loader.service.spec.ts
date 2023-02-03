/*
 * Copyright 2020-2023 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { MainLoaderService } from './main-loader.service';

describe('MainLoaderService', () => {
  let service: MainLoaderService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MainLoaderService,
      ],
    });

    service = TestBed.inject(MainLoaderService);
  });

  it('should set make the loader visible initially', () => {
    expect(service.isVisible).toBeTruthy();
  });

  it('should hide the loader', () => {
    service.hide();

    expect(service.isVisible).toBeFalsy();
  });

  describe('when the loader is hidden', () => {
    beforeEach(() => {
      service.hide();
    });

    it('should show the loader', () => {
      service.show();

      expect(service.isVisible).toBeTruthy();
    });
  });
});
