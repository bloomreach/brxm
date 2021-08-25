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

import { async, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Subject } from 'rxjs';
import { first } from 'rxjs/operators';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';
import { SiteService } from './site.service';

describe('SiteService', () => {
  let service: SiteService;
  let app1UpdateSelectedSite: jasmine.Spy;
  let app2UpdateSelectedSite: jasmine.Spy;
  let app3UpdateSelectedSite: jasmine.Spy;
  let clientAppMocks: ClientAppMock[];
  let windowRefMock: WindowRef;

  const updateSelectedSite$ = new Subject();

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

  const sitesMock: Site[] = [
    {
      siteId: 1,
      accountId: 456,
      isNavappEnabled: true,
      name: 'myTestSite',
      subGroups: [
        {
          siteId: 2,
          accountId: 123,
          isNavappEnabled: true,
          name: 'myTestSite2',
        },
      ],
    },
    {
      siteId: 3,
      accountId: 890,
      isNavappEnabled: true,
      name: 'myTestSite3',
    },
  ];

  const selectedSiteIdMock: SiteId = {
    siteId: 2,
    accountId: 123,
  };

  beforeEach(() => {
    const busyIndicatorServiceMock = jasmine.createSpyObj('BusyIndicatorService', [
      'show',
      'hide',
    ]);

    app1UpdateSelectedSite = jasmine
      .createSpy('app1 updateSelectedSite')
      .and
      .returnValue(Promise.resolve());

    app2UpdateSelectedSite = jasmine
      .createSpy('app2 updateSelectedSite')
      .and
      .returnValue(Promise.resolve());

    app3UpdateSelectedSite = jasmine
      .createSpy('app3 updateSelectedSite')
      .and
      .returnValue(Promise.resolve());

    clientAppMocks = [
      new ClientAppMock({
        url: 'https://abc.com/sm/testApp1',
        api: { updateSelectedSite: app1UpdateSelectedSite },
      }),
      new ClientAppMock({
        url: 'https://abc.com/sm/testApp2',
        api: { updateSelectedSite: app2UpdateSelectedSite },
      }),
      new ClientAppMock({
        url: 'https://abc.com/sm/testApp3',
        api: { updateSelectedSite: app3UpdateSelectedSite },
      }),
    ];

    const clientAppServiceMock = {
      apps: clientAppMocks,
      activeApp: clientAppMocks[1],
    };

    windowRefMock = {
      nativeWindow: {
        location: {
          href: 'https://abc.com/navapp/',
          reload: jasmine.createSpy('reload'),
          assign: jasmine.createSpy('assign'),
        },
      } as any,
    };

    const connectionServiceMock = {
      updateSelectedSite$: updateSelectedSite$.asObservable(),
    };

    TestBed.configureTestingModule({
      providers: [
        SiteService,
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: WindowRef, useValue: windowRefMock },
        { provide: NGXLogger, useValue: loggerMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
      ],
    });

    service = TestBed.inject(SiteService);
  });

  it('should initialize the service', fakeAsync(() => {
    service.selectedSite$.pipe(
      first(),
    ).subscribe(site => {
      expect(site.siteId).toEqual(selectedSiteIdMock.siteId);
    });

    service.init(sitesMock, selectedSiteIdMock);

    expect(service.sites).toEqual(sitesMock);

    tick();
  }));

  describe('when the service is initialized', () => {
    beforeEach(() => {
      service.init(sitesMock, selectedSiteIdMock);
    });

    it('should subscribe to child updateSelectedSite events', () => {
      const siteId: SiteId = {
        siteId: 2,
        accountId: 123,
      };
      const site: Site = sitesMock[0].subGroups[0];

      service.selectedSite$.subscribe(updatedSite => {
        expect(updatedSite).toEqual(site);
      });

      updateSelectedSite$.next(siteId);
    });

    it('should update the selected site for the active app', () => {
      const site: Site = {
        siteId: 2,
        accountId: 123,
        name: 'some name',
        isNavappEnabled: true,
      };

      service.updateSelectedSite(site);

      expect(app2UpdateSelectedSite).toHaveBeenCalledWith(site);
    });

    it('should redirect to iUI if navapp isn\'t enabled for the selected site', async () => {
      const site: Site = {
        siteId: 2,
        accountId: 123,
        name: 'some name',
        isNavappEnabled: false,
      };

      await service.updateSelectedSite(site);
      expect(app2UpdateSelectedSite).toHaveBeenCalledWith(site);
      expect(windowRefMock.nativeWindow.location.assign).toHaveBeenCalledWith('https://abc.com/testApp2');
    });

    describe('logging', () => {
      describe('updateSelectedSite()', () => {
        const site: Site = {
          siteId: 2,
          accountId: 123,
          name: 'some name',
          isNavappEnabled: true,
        };

        beforeEach(waitForAsync(() => {
          service.updateSelectedSite(site);
        }));

        it('should log that updateSelectedSite() is called for the active app', () => {
          expect(loggerMock.debug).toHaveBeenCalledWith(
            'updateSelectedSite() is called for the active app \'https://abc.com/sm/testApp2\'',
            site,
          );
        });
      });
    });
  });
});
