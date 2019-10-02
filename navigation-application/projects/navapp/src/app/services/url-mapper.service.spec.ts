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
import { InternalError } from '../error-handling/models/internal-error';
import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { NavItemMock } from '../models/dto/nav-item.mock';

import { APP_SETTINGS } from './app-settings';
import { NavItemService } from './nav-item.service';
import { UrlMapperService } from './url-mapper.service';

describe('UrlMapperService', () => {
  let urlMapperService: UrlMapperService;

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
  const navItemServiceMock = {
    navItems: navItemsMock,
  };

  let clientAppServiceMock = {
    activeApp: {
      url: '',
    },
  };

  const appSettingsMock = new AppSettingsMock({
    navAppBaseURL: 'https://some-domain.com/base/path',
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        UrlMapperService,
        { provide: APP_SETTINGS, useValue: appSettingsMock },
        { provide: NavItemService, useValue: navItemServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
      ],
    });

    urlMapperService = TestBed.get(UrlMapperService);
    clientAppServiceMock = TestBed.get(ClientAppService);
  });

  it('should return the base path', () => {
    const expected = '/base/path';

    const actual = urlMapperService.basePath;

    expect(actual).toBe(expected);
  });

  it('should trim the leading slash', () => {
    const expected = 'some/path';

    const actual = urlMapperService.trimLeadingSlash('/some/path');

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url', () => {
    const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = urlMapperService.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url with stripped off "iframe" path part from iframe url', () => {
    const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/iframe/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = urlMapperService.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url with stripped off base path part from iframe url', () => {
    const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/base/path/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = urlMapperService.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav item to the browser url with stripped off "iframe" and base path parts from iframe url', () => {
    const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
    const navItem: NavItem = {
      id: 'some-id',
      appIframeUrl: 'https://iframe-domain.com/base/path/iframe/path/to/app',
      appPath: 'path/to/page?param1=value1#hash-data',
    };

    const actual = urlMapperService.mapNavItemToBrowserUrl(navItem);

    expect(actual).toBe(expected);
  });

  it('should map nav location to the browser url', () => {
    const expected = ['/base/path/iframe1/url/app/path/to/page1/some/detailed/page?param1=value1#hash-data', navItemsMock[1]];
    const navLocation: NavLocation = {
      path: 'app/path/to/page1/some/detailed/page?param1=value1#hash-data',
    };

    const actual = urlMapperService.mapNavLocationToBrowserUrl(navLocation);

    expect(actual).toEqual(expected);
  });

  it('should throw an exception during mapNavLocationToBrowserUrl call when the active app is not set', () => {
    const expected = new InternalError('Initialization problem', 'Active app is not set');

    clientAppServiceMock.activeApp = undefined;

    expect(() => urlMapperService.mapNavLocationToBrowserUrl({ path: '' })).toThrow(expected);
  });

  it('should throw an exception when the nav item contains a relative url instead of an absolute one', () => {
    const expectedError = new InternalError(undefined, 'The url has incorrect format: /some/url');

    const navItem: NavItem = {
      id: 'some-id',
      appIframeUrl: '/some/url',
      appPath: 'path/to/page',
    };

    expect(() => urlMapperService.mapNavItemToBrowserUrl(navItem)).toThrow(expectedError);
  });
});
