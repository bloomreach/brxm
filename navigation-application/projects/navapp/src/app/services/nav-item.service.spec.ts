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

import { NavItemDtoMock } from '../models/dto/nav-item-dto.mock';

import { NavItemService } from './nav-item.service';

describe('NavItemService', () => {
  const joc = jasmine.objectContaining;
  let service: NavItemService;

  const appIframeUrl = 'https://test.url/some/path';
  const mockNavItemDtos = [
    new NavItemDtoMock({
      appPath: 'a',
      appIframeUrl,
    }),
    new NavItemDtoMock({
      appPath: 'a/b/c',
      appIframeUrl,
    }),
    new NavItemDtoMock({
      appPath: 'a/b',
      appIframeUrl,
    }),
    new NavItemDtoMock({
      appPath: 'a/b/c/d',
      appIframeUrl,
    }),
    new NavItemDtoMock({
      appPath: 'test1',
      appIframeUrl,
    }),
    new NavItemDtoMock({
      appPath: 'test2',
      appIframeUrl,
    }),
  ];

  const loggerMock = jasmine.createSpyObj('NGXLogger', [
    'warn',
    'debug',
  ]);

  beforeEach(() => {
    service = new NavItemService(loggerMock);
  });

  it('should sort navItems by appPath length', () => {
    const expected = [
      joc({
        appPath: 'a/b/c/d',
        appIframeUrl,
      }),
      joc({
        appPath: 'a/b/c',
        appIframeUrl,
      }),
      joc({
        appPath: 'test1',
        appIframeUrl,
      }),
      joc({
        appPath: 'test2',
        appIframeUrl,
      }),
      joc({
        appPath: 'a/b',
        appIframeUrl,
      }),
      joc({
        appPath: 'a',
        appIframeUrl,
      }),
    ];

    service.registerNavItemDtos(mockNavItemDtos);

    expect(service.navItems.length).toBe(6);
    expect(service.navItems).toEqual(expected);
  });

  it('should return an empty list of nav items', () => {
    const expected = [];

    const actual = service.navItems;

    expect(actual).toEqual(expected);
  });

  describe('when nav items are registered', () => {
    beforeEach(() => {
      service.registerNavItemDtos(mockNavItemDtos);
    });

    it('should log that', () => {
      expect(loggerMock.debug).toHaveBeenCalledWith('Register nav items', mockNavItemDtos);
    });

    it('should find a nav item by an iframe url and an app path', () => {
      const expected = joc({
        appPath: 'a/b',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b', appIframeUrl);

      expect(actual).toEqual(expected);
    });

    it('should find a nav item by an iframe url and the app path longer than defined in nav items', () => {
      const expected = joc({
        appPath: 'a/b',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b/test/test', appIframeUrl);

      expect(actual).toEqual(expected);
    });

    it('should find a nav item by an app path', () => {
      const expected = joc({
        appPath: 'a/b/c/d',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b/c/d/test/test');

      expect(actual).toEqual(expected);
    });

    it('should find a nav item by an iframe path and an app path', () => {
      const expected = joc({
        appPath: 'a/b/c/d',
        appIframeUrl,
      });

      const actual = service.findNavItem('a/b/c/d/test/test', '/some/path');

      expect(actual).toBeDefined();
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
      const dtos = [
        new NavItemDtoMock({
          appPath: 'a',
          appIframeUrl: 'test.url/some/path',
        }),
        new NavItemDtoMock({
          appPath: 'a/b/c',
          appIframeUrl: '/some/path',
        }),
        new NavItemDtoMock({
          appPath: 'b/c',
          appIframeUrl: 'https://test.com/base',
        }),
      ];

      service.registerNavItemDtos(dtos);
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
      const expected = joc({
        appPath: 'b/c',
        appIframeUrl: 'https://test.com/base',
      });

      const actual = service.findNavItem('b/c', 'https://test.com/base');

      expect(actual).toBeDefined();
      expect(actual).toEqual(expected);
    });
  });
});
