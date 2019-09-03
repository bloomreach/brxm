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
import { NavItem, NavLocation } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services/client-app.service';
import { NavItemMock } from '../models/dto/nav-item.mock';
import { GlobalSettingsService } from '../services/global-settings.service';
import { NavConfigService } from '../services/nav-config.service';

import { UrlMapperService } from './url-mapper.service';

describe('UrlMapperService', () => {
  let service: UrlMapperService;

  const globalSettingsServiceMock = {
    appSettings: {
    },
  } as any;

  const navItemsMock = [
    new NavItemMock({
      id: 'item1',
      appIframeUrl: 'http://domain.com/iframe1/url',
      appPath: 'app/path/to/home',
    }),
    new NavItemMock({
      id: 'item2',
      appIframeUrl: 'http://domain.com/iframe1/url',
      appPath: 'app/path/to/page1',
    }),
    new NavItemMock({
      id: 'item3',
      appIframeUrl: 'http://domain.com/iframe2/url',
      appPath: 'another/app/path/to/home',
    }),
  ];
  const navConfigServiceMock = {
    navItems: navItemsMock,
  };

  const clientAppServiceMock = {
    activeApp: {
      url: '',
    },
  } as any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        UrlMapperService,
        { provide: GlobalSettingsService, useValue: globalSettingsServiceMock },
        { provide: NavConfigService, useValue: navConfigServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
      ],
    });

    service = TestBed.get(UrlMapperService);
  });

  it('should combine path parts', () => {
    const expected = '/part1/part2/part3';

    const actual = service.combinePathParts('part1', 'part2/', '/part3');

    expect(actual).toBe(expected);
  });

  it('should trim the leading slash', () => {
    const expected = 'some/path';

    const actual = service.trimLeadingSlash('/some/path');

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url', () => {
    const expected = '/context-path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem  = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = service.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url with stripped of "cms" path part from iframe url', () => {
    const expected = '/context-path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem  = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/cms/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = service.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url with stripped of "iframe" path part from iframe url', () => {
    const expected = '/context-path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem  = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/iframe/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = service.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav location to the browser url', () => {
    const expected = ['/context-path/iframe1/url/app/path/to/page1/some/detailed/page?param1=value1#hash-data', navItemsMock[1]];
    const navLocation: NavLocation  = {
      path: 'app/path/to/page1/some/detailed/page?param1=value1#hash-data',
    };

    const actual = service.mapNavLocationToBrowserUrl(navLocation);

    expect(actual).toEqual(expected);
  });
});
