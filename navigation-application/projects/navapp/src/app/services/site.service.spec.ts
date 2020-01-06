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

import { async, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Site, SiteId } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

import { ClientAppMock } from '../client-app/models/client-app.mock';
import { ClientAppService } from '../client-app/services/client-app.service';
import { WindowRef } from '../shared/services/window-ref.service';

import { BusyIndicatorService } from './busy-indicator.service';
import { SiteService } from './site.service';

describe('SiteService', () => {
  let siteService: SiteService;
  let app1UpdateSelectedSite: jasmine.Spy;
  let app2UpdateSelectedSite: jasmine.Spy;
  let app3UpdateSelectedSite: jasmine.Spy;
  let clientAppMocks: ClientAppMock[];
  let windowRefMock: WindowRef;

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'debug',
  ]);

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
        url: 'testApp1',
        api: { updateSelectedSite: app1UpdateSelectedSite },
      }),
      new ClientAppMock({
        url: 'testApp2',
        api: { updateSelectedSite: app2UpdateSelectedSite },
      }),
      new ClientAppMock({
        url: 'testApp3',
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
          href: '/navapp/some/path',
          reload: jasmine.createSpy('reload'),
          assign: jasmine.createSpy('assign'),
        },
      } as any,
    };

    const mockSites: Site[] = [
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

    TestBed.configureTestingModule({
      providers: [
        SiteService,
        { provide: BusyIndicatorService, useValue: busyIndicatorServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: WindowRef, useValue: windowRefMock },
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

  it('should update the selected site for the active app', () => {
    const site: Site = {
      siteId: 2,
      accountId: 123,
      name: 'some name',
      isNavappEnabled: true,
    };

    siteService.updateSelectedSite(site);

    expect(app2UpdateSelectedSite).toHaveBeenCalledWith(site);
  });

  it('should broadcast to non-active apps that the selected site should be updated', fakeAsync(() => {
    const site: Site = {
      siteId: 2,
      accountId: 123,
      name: 'some name',
      isNavappEnabled: true,
    };

    siteService.updateSelectedSite(site);

    tick();

    expect(app1UpdateSelectedSite).toHaveBeenCalledWith();
    expect(app3UpdateSelectedSite).toHaveBeenCalledWith();

    expect(app2UpdateSelectedSite).toHaveBeenCalledBefore(app1UpdateSelectedSite);
    expect(app2UpdateSelectedSite).toHaveBeenCalledBefore(app3UpdateSelectedSite);
  }));

  it('should redirect to iUI if navapp isn\'t enabled for the selected site', () => {
    const site: Site = {
      siteId: 2,
      accountId: 123,
      name: 'some name',
      isNavappEnabled: false,
    };

    siteService.updateSelectedSite(site);

    expect(windowRefMock.nativeWindow.location.assign).toHaveBeenCalledWith('/some/path');
  });

  describe('logging', () => {
    describe('updateSelectedSite()', () => {
      const site: Site = {
        siteId: 2,
        accountId: 123,
        name: 'some name',
        isNavappEnabled: true,
      };

      beforeEach(async(() => {
        siteService.updateSelectedSite(site);
      }));

      it('should log that updateSelectedSite() is called for the active app', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('updateSelectedSite() is called for the active app \'testApp2\'', site);
      });

      it('should log that updateSelectedSite() is called for the other apps', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('updateSelectedSite() is called for \'testApp1\'');
        expect(loggerMock.debug).toHaveBeenCalledWith('updateSelectedSite() is called for \'testApp3\'');
      });

      it('should log updateSelectedSite() broadcasting has been finished successfully', () => {
        expect(loggerMock.debug).toHaveBeenCalledWith('updateSelectedSite() broadcasting finished successfully');
      });
    });
  });
});
