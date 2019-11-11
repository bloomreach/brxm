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

import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { SiteService } from './site.service';

describe('SiteService', () => {
  let siteService: SiteService;
  let updateSelectedSite: jasmine.Spy;
  let clientAppMocks: ClientAppMock[];

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

  beforeEach(() => {
    const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    updateSelectedSite = jasmine
      .createSpy('updateSelectedSite')
      .and
      .returnValue(Promise.resolve());

    clientAppMocks = [
      new ClientAppMock({
        url: 'testApp1',
        api: { updateSelectedSite },
      }),
      new ClientAppMock({
        url: 'testApp2',
        api: { updateSelectedSite },
      }),
      new ClientAppMock({
        url: 'testApp3',
        api: { updateSelectedSite },
      }),
    ];

    const clientAppServiceMock = {
      apps: clientAppMocks,
      activeApp: clientAppMocks[1],
    };

    const mockSites: Site[] = [
      {
        siteId: 1,
        accountId: 456,
        name: 'myTestSite',
        subGroups: [
          {
            siteId: 2,
            accountId: 123,
            name: 'myTestSite2',
          },
        ],
      },
      {
        siteId: 3,
        accountId: 890,
        name: 'myTestSite3',
      },
    ];

    TestBed.configureTestingModule({
      providers: [
        SiteService,
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: NGXLogger, useValue: loggerMock },
      ],
    });

    siteService = TestBed.get(SiteService);
    siteService.sites = mockSites;
  });

  it('should set selected site', fakeAsync(() => {
    const siteId: SiteId = {
      siteId: 2,
      accountId: 123,
    };

    siteService.selectedSite$.subscribe(site => {
      expect(site.siteId).toEqual(siteId.siteId);
    });

    siteService.setSelectedSite(siteId);

    tick();
  }));

  it('should communicate to non-active apps to update the site after the active app has updated', fakeAsync(() => {
    const siteId: SiteId = {
      siteId: 2,
      accountId: 123,
    };

    siteService.updateSelectedSite(siteId);

    tick();

    expect(updateSelectedSite).toHaveBeenCalledTimes(clientAppMocks.length);
  }));
});
