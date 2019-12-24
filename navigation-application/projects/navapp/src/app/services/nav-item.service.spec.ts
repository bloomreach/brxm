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

import { NavItemMock } from '../models/dto/nav-item.mock';

import { NavItemService } from './nav-item.service';

describe('NavItemService', () => {
  let service: NavItemService;

  const appIframeUrl = 'https://test.url/some/path';
  const mockNavItems = [
    new NavItemMock({
      appPath: 'a',
      appIframeUrl,
    }),
    new NavItemMock({
      appPath: 'a/b/c',
      appIframeUrl,
    }),
    new NavItemMock({
      appPath: 'a/b',
      appIframeUrl,
    }),
    new NavItemMock({
      appPath: 'a/b/c/d',
      appIframeUrl,
    }),
    new NavItemMock({
      appPath: 'test1',
      appIframeUrl,
    }),
    new NavItemMock({
      appPath: 'test2',
      appIframeUrl,
    }),
  ];

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'warn',
  ]);

  beforeEach(() => {
    service = new NavItemService(loggerMock);
  });

  it('should sort navItems by appPath length', () => {
    const expected = [
      new NavItemMock({
        appPath: 'a/b/c/d',
        appIframeUrl,
      }),
      new NavItemMock({
        appPath: 'a/b/c',
        appIframeUrl,
      }),
      new NavItemMock({
        appPath: 'test1',
        appIframeUrl,
      }),
      new NavItemMock({
        appPath: 'test2',
        appIframeUrl,
      }),
      new NavItemMock({
        appPath: 'a/b',
        appIframeUrl,
      }),
      new NavItemMock({
        appPath: 'a',
        appIframeUrl,
      }),
    ];

    service.navItems = mockNavItems;

    expect(service.navItems).toEqual(expected);
  });

  describe('when nav items are set', () => {
    beforeEach(() => {
      service.navItems = mockNavItems;
    });

    it('should find a nav item by an iframe url and an app path', () => {
      const expected = new NavItemMock({
        appPath: 'a/b',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b', appIframeUrl);

      expect(actual).toEqual(expected);
    });

    it('should find a nav item by an iframe url and the app path longer than defined in nav items', () => {
      const expected = new NavItemMock({
        appPath: 'a/b',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b/test/test', appIframeUrl);

      expect(actual).toEqual(expected);
    });

    it('should find a nav item by an app path', () => {
      const expected = new NavItemMock({
        appPath: 'a/b/c/d',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b/c/d/test/test');

      expect(actual).toEqual(expected);
    });

    it('should find a nav item by an iframe path and an app path', () => {
      const expected = new NavItemMock({
        appPath: 'a/b/c/d',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b/c/d/test/test', '/some/path');

      expect(actual).toEqual(expected);
    });

    it('should not find a nav item by an iframe url and an app path', () => {
      const actual = service.findNavItem('something/unknown', appIframeUrl);

      expect(actual).toBeUndefined();
    });

    it('should not find a nav item by an app path', () => {
      const actual = service.findNavItem('something/unknown');

      expect(actual).toBeUndefined();
    });
  });

  describe('when nav items with wrong iframe urls are set', () => {
    beforeEach(() => {
      service.navItems = [
        new NavItemMock({
          appPath: 'a',
          appIframeUrl: 'test.url/some/path',
        }),
        new NavItemMock({
          appPath: 'a/b/c',
          appIframeUrl: '/some/path',
        }),
        new NavItemMock({
          appPath: 'b/c',
          appIframeUrl: 'https://test.com/base',
        }),
      ];
    });

    it('should not throw an exception', () => {
      expect(() => service.findNavItem('a', 'test.url/some/path')).not.toThrow();
    });

    it('should output a warning message', () => {
      service.findNavItem('a', 'test.url/some/path');

      expect(loggerMock.warn).toHaveBeenCalledWith('Unable to parse nav items\'s url "test.url/some/path"');
    });

    it('should not find a nav item by an iframe url and an app path when the protocol is omitted in the iframe url', () => {
      const actual = service.findNavItem('a', 'test.url/some/path');

      expect(actual).toBeUndefined();
    });

    it('should not find a nav item by an iframe url and an app path when the origin is omitted in the iframe url', () => {
      const actual = service.findNavItem('a/b/c', '/some/path');

      expect(actual).toBeUndefined();
    });

    it('should find a nav with the correct iframe url', () => {
      const expected = new NavItemMock({
        appPath: 'b/c',
        appIframeUrl: 'https://test.com/base',
      });

      const actual = service.findNavItem('b/c', 'https://test.com/base');

      expect(actual).toEqual(expected);
    });
  });
});
