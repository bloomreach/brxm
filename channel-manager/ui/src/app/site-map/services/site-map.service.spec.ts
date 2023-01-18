/*
 * Copyright 2022-2023 Bloomreach
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

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../services/ng1/channel.ng1.service';
import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../../services/ng1/config.ng1.service';
import { SnackBarService } from '../../services/snack-bar.service';
import { SiteMapItemMock } from '../models/site-map-item.model.mock';

import { SiteMapService } from './site-map.service';

describe('SiteMapService', () => {
  let service: SiteMapService;
  let httpTestingController: HttpTestingController;

  const cmsUser = 'admin';
  const channel = {
    mountId: 'test',
    contextPath: '/test/',
    hostGroup: 'test',
  };
  const contextPath = '/cms';

  const configServiceMock: Partial<Ng1ConfigService> = {
    cmsUser,
    getCmsContextPath: () => contextPath,
  };

  const channelServiceMock: Partial<Ng1ChannelService> = {
    getChannel: () => channel,
  };

  const snackBarServiceMock = {
    showSnackBar: jest.fn(),
  };

  const mockSiteMapTree = [
    new SiteMapItemMock({
      children: [
        new SiteMapItemMock({
          id: 'idTest2',
          name: 'nameTest2',
          pageTitle: null,
          pathInfo: 'idTest2',
          renderPathInfo: '/idTest2',
          children: [
            new SiteMapItemMock({
              id: 'idTest3',
              name: 'nameTest3',
              pageTitle: 'pageTitleTest3',
              pathInfo: 'idTest2/idTest3',
              renderPathInfo: '/idTest3',
            }),
          ],
        }),
        new SiteMapItemMock({
          id: 'idTest4',
          name: 'nameTest4',
          pageTitle: 'pageTitleTest',
          pathInfo: 'idTest4',
          renderPathInfo: '/idTest4',
        }),
      ],
    }),
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [
        { provide: NG1_CONFIG_SERVICE, useValue: configServiceMock },
        { provide: NG1_CHANNEL_SERVICE, useValue: channelServiceMock },
        { provide: SnackBarService, useValue: snackBarServiceMock },
      ],
    });
    service = TestBed.inject(SiteMapService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should load search items by search query', () => {
    const siteMapId = 'siteMapId';
    const searchQuery = 'test';
    service.search(siteMapId, searchQuery);

    const req = httpTestingController.expectOne(`${contextPath}/_rp/${siteMapId}./search?fq=${searchQuery}`);
    req.flush({ data: mockSiteMapTree });
  });

  it('should get correct headers', () => {
    const headers = service.getHeaders();

    expect(headers).toEqual({
      'CMS-User': cmsUser,
      contextPath: channel.contextPath,
      hostGroup: channel.hostGroup,
    });
  });

  it('should load site map items', () => {
    const siteMapId = 'siteMapId';
    service.load(siteMapId);

    const req = httpTestingController.expectOne(`${contextPath}/_rp/${siteMapId}./sitemapitem`);
    req.flush({ data: mockSiteMapTree });
  });

  describe('should load site map item', () => {
    it('with ancestry true', () => {
      const siteMapId = 'siteMapId';
      const path = 'path/item1';
      service.loadItem(siteMapId, path, false, true, false);

      const req = httpTestingController.expectOne(`${contextPath}/_rp/${siteMapId}./sitemapitem/${path}?ancestry=true`);
      req.flush({ data: mockSiteMapTree });
    });

    it('with ancestry false', () => {
      const siteMapId = 'siteMapId';
      const path = 'path/item1';
      service.loadItem(siteMapId, path, false, false, false);

      const req = httpTestingController.expectOne(`${contextPath}/_rp/${siteMapId}./sitemapitem/${path}?ancestry=false`);
      req.flush(mockSiteMapTree);
    });
  });
});
