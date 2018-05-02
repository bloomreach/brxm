/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import angular from 'angular';
import 'angular-mocks';

describe('ExtensionService', () => {
  let ConfigService;
  let ExtensionService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ConfigService = {};

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', ConfigService);
    });

    inject((_ExtensionService_) => {
      ExtensionService = _ExtensionService_;
    });
  });

  describe('hasExtensions', () => {
    it('knows when there are any extensions with certain context', () => {
      ConfigService.extensions = [{ context: 'a' }, { context: 'b' }];
      expect(ExtensionService.hasExtensions('a')).toBe(true);
      expect(ExtensionService.hasExtensions('b')).toBe(true);
      expect(ExtensionService.hasExtensions('c')).toBe(false);
    });

    it('works when there are zero extensions', () => {
      ConfigService.extensions = [];
      expect(ExtensionService.hasExtensions('a')).toBe(false);
    });

    it('works when there are no extensions defined', () => {
      ConfigService.extensions = undefined;
      expect(ExtensionService.hasExtensions('a')).toBe(false);
    });
  });

  describe('getExtensions', () => {
    it('returns all extensions with a certain context', () => {
      const extension1 = { id: '1', context: 'a' };
      const extension2 = { id: '2', context: 'b' };
      const extension3 = { id: '3', context: 'a' };

      ConfigService.extensions = [extension1, extension2, extension3];

      expect(ExtensionService.getExtensions('a')).toEqual([extension1, extension3]);
      expect(ExtensionService.getExtensions('b')).toEqual([extension2]);
      expect(ExtensionService.getExtensions('c')).toEqual([]);
    });

    it('works when there are zero extensions', () => {
      ConfigService.extensions = [];
      expect(ExtensionService.getExtensions('a')).toEqual([]);
    });

    it('works when there are no extensions defined', () => {
      ConfigService.extensions = undefined;
      expect(ExtensionService.getExtensions('a')).toEqual([]);
    });
  });
});
