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
import { NavLocation } from '@bloomreach/navapp-communication';

import { ClientAppService } from '../client-app/services/client-app.service';
import { InternalError } from '../error-handling/models/internal-error';
import { AppSettingsMock } from '../models/dto/app-settings.mock';
import { NavItemMock } from '../models/nav-item.mock';

import { APP_SETTINGS } from './app-settings';
import { NavItemService } from './nav-item.service';
import { UrlMapperService } from './url-mapper.service';

describe('UrlMapperService', () => {
  let service: UrlMapperService;

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
  const navItemServiceMock = jasmine.createSpyObj('NavItemService', [
    'findNavItem',
  ]);
  navItemServiceMock.navItems = navItemsMock;

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

    service = TestBed.inject(UrlMapperService);
    clientAppServiceMock = TestBed.inject(ClientAppService);
  });

  it('should return the base path', () => {
    const expected = '/base/path';

    const actual = service.basePath;

    expect(actual).toBe(expected);
  });

  it('should trim the leading slash', () => {
    const expected = 'some/path';

    const actual = service.trimLeadingSlash('/some/path');

    expect(actual).toBe(expected);
  });

  describe('extractPathAndQueryStringAndHash', () => {
    it('should extract the path part the query string part with hash', () => {
      const expected = ['some/path', '?param=value#some-hash'];

      const actual = service.extractPathAndQueryStringAndHash('some/path?param=value#some-hash');

      expect(actual).toEqual(expected);
    });

    it('should extract the path part and the query string only if there is no hash', () => {
      const expected = ['some/path', '?param=value'];

      const actual = service.extractPathAndQueryStringAndHash('some/path?param=value');

      expect(actual).toEqual(expected);
    });

    it('should extract the path part and the hash only if there is no a query string', () => {
      const expected = ['some/path', '#some-hash'];

      const actual = service.extractPathAndQueryStringAndHash('some/path#some-hash');

      expect(actual).toEqual(expected);
    });

    it('should extract the path part only and an empty query string with hash if there is no a query string and hash', () => {
      const expected = ['some/path', ''];

      const actual = service.extractPathAndQueryStringAndHash('some/path');

      expect(actual).toEqual(expected);
    });

    it('should preserve the leading slash', () => {
      const expected = ['/some/path', '?param'];

      const actual = service.extractPathAndQueryStringAndHash('/some/path?param');

      expect(actual).toEqual(expected);
    });

    it('should preserve the trailing slash', () => {
      const expected = ['some/path/', '?param'];

      const actual = service.extractPathAndQueryStringAndHash('some/path/?param');

      expect(actual).toEqual(expected);
    });
  });

  describe('mapNavItemToBrowserUrl', () => {
    it('should map nav item to the browser url', () => {
      const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
      const navItem = new NavItemMock({
        id: 'some-id',
        appIframeUrl: 'https://iframe-domain.com/path/to/app',
        appPath: 'path/to/page?param1=value1#hash-data',
      });

      const actual = service.mapNavItemToBrowserUrl(navItem);

      expect(actual).toBe(expected);
    });

    it('should map nav item to the browser url with stripped off "iframe" path part from iframe url', () => {
      const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
      const navItem = new NavItemMock({
        id: 'some-id',
        appIframeUrl: 'https://iframe-domain.com/iframe/path/to/app',
        appPath: 'path/to/page?param1=value1#hash-data',
      });

      const actual = service.mapNavItemToBrowserUrl(navItem);

      expect(actual).toBe(expected);
    });

    it('should map nav item to the browser url with stripped off base path part from iframe url', () => {
      const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
      const navItem = new NavItemMock({
        id: 'some-id',
        appIframeUrl: 'https://iframe-domain.com/base/path/path/to/app',
        appPath: 'path/to/page?param1=value1#hash-data',
      });

      const actual = service.mapNavItemToBrowserUrl(navItem);

      expect(actual).toBe(expected);
    });

    it('should map nav item to the browser url with stripped off "iframe" and base path parts from iframe url', () => {
      const expected = '/base/path/path/to/app/path/to/page?param1=value1#hash-data';
      const navItem = new NavItemMock({
        id: 'some-id',
        appIframeUrl: 'https://iframe-domain.com/base/path/iframe/path/to/app',
        appPath: 'path/to/page?param1=value1#hash-data',
      });

      const actual = service.mapNavItemToBrowserUrl(navItem);

      expect(actual).toBe(expected);
    });

    it('should throw an exception when the nav item contains a relative url instead of an absolute one', () => {
      const expectedError = new InternalError(undefined, 'The url has incorrect format: /some/url');

      const navItem = new NavItemMock({
        id: 'some-id',
        appIframeUrl: '/some/url',
        appPath: 'path/to/page',
      });

      expect(() => service.mapNavItemToBrowserUrl(navItem)).toThrow(expectedError);
    });
  });

  describe('mapNavLocationToBrowserUrl', () => {
    it('should map nav location to the browser url', () => {
      const expected = ['/base/path/iframe1/url/app/path/to/page1?param1=value1#hash-data', navItemsMock[1]];

      navItemServiceMock.findNavItem.and.returnValue(navItemsMock[1]);
      const navLocation: NavLocation = {
        path: 'app/path/to/page1?param1=value1#hash-data',
      };

      const actual = service.mapNavLocationToBrowserUrl(navLocation);

      expect(actual).toEqual(expected);
    });

    it('should map nav location with sub path part to the browser url', () => {
      const expected = ['/base/path/iframe1/url/app/path/to/page1/some/detailed/page?param1=value1#hash-data', navItemsMock[1]];

      navItemServiceMock.findNavItem.and.returnValue(navItemsMock[1]);
      const navLocation: NavLocation = {
        path: 'app/path/to/page1/some/detailed/page?param1=value1#hash-data',
      };

      const actual = service.mapNavLocationToBrowserUrl(navLocation);

      expect(actual).toEqual(expected);
    });

    it('should use pathPrefix when searching for a nav item', () => {
      navItemServiceMock.findNavItem.and.returnValue(navItemsMock[1]);
      const navLocation: NavLocation = {
        pathPrefix: '/some/path/prefix',
        path: 'app/path/to/page1',
      };

      service.mapNavLocationToBrowserUrl(navLocation);

      expect(navItemServiceMock.findNavItem).toHaveBeenCalledWith('app/path/to/page1', '/some/path/prefix');
    });

    it('should throw an exception during mapNavLocationToBrowserUrl invocation when the active app is not set' +
      ' and the current app is used', () => {
      const expected = new InternalError('Initialization problem', 'Active app is not set');

      clientAppServiceMock.activeApp = undefined;

      expect(() => service.mapNavLocationToBrowserUrl({ path: '' }, true)).toThrow(expected);
    });
  });
});
