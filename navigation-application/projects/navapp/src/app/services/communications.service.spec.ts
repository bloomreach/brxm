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
import { ChildPromisedApi } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services';

import { CommunicationsService } from './communications.service';
import { OverlayService } from './overlay.service';

describe('CommunicationsService', () => {
  function setup(): {
    communicationsService: CommunicationsService;
    overlayService: OverlayService;
    clientAppService: ClientAppService;
    api: ChildPromisedApi;
  } {
    const overlayService = jasmine.createSpyObj({
      enable: jasmine.createSpy(),
      disable: jasmine.createSpy(),
    });

    const api = jasmine.createSpyObj({
      navigate: Promise.resolve(),
    });

    const clientAppService = jasmine.createSpyObj('clientAppService', {
      getApp: {
        api,
      },
      activateApplication: jasmine.createSpy(),
    });

    TestBed.configureTestingModule({
      providers: [
        CommunicationsService,
        { provide: OverlayService, useValue: overlayService },
        { provide: ClientAppService, useValue: clientAppService },
      ],
    });

    return {
      communicationsService: TestBed.get(CommunicationsService),
      overlayService: TestBed.get(OverlayService),
      clientAppService: TestBed.get(ClientAppService),
      api,
    };
  }

  it('should navigate', () => {
    const { communicationsService, clientAppService, api } = setup();
    const url = 'url';
    const path = 'test/test';

    expect(communicationsService).toBeTruthy();

    communicationsService.navigate(url, path).then(() => {
      expect(api.navigate).toHaveBeenCalledWith({ path });
      expect(clientAppService.activateApplication).toHaveBeenCalledWith(url);
    });
  });
});
