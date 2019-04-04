/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
  let $window;
  let ConfigService;
  let ExtensionService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', jasmine.createSpyObj('ConfigService', ['getCmsContextPath']));
    });

    inject((_$window_, _ConfigService_, _ExtensionService_) => {
      $window = _$window_;
      ConfigService = _ConfigService_;
      ExtensionService = _ExtensionService_;
    });
  });

  describe('hasExtensions', () => {
    it('knows when there are any extensions with a certain extensionPoint', () => {
      ConfigService.extensions = [{ extensionPoint: 'a' }, { extensionPoint: 'b' }];
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
    it('returns all extensions with a certain extensionPoint', () => {
      const extension1 = { id: '1', extensionPoint: 'a' };
      const extension2 = { id: '2', extensionPoint: 'b' };
      const extension3 = { id: '3', extensionPoint: 'a' };

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

  describe('getExtensionUrl', () => {
    beforeEach(() => {
      $window.location = { origin: 'https://www.example.com:443' };
      ConfigService.antiCache = 42;
    });

    describe('for extensions from the same origin', () => {
      it('works when the CMS location has a context path', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        expect(ExtensionService.getExtensionUrl({ url: '/testUrl' })).toEqual('https://www.example.com/cms/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });

      it('works when the CMS location has no context path', () => {
        ConfigService.getCmsContextPath.and.returnValue('/');
        expect(ExtensionService.getExtensionUrl({ url: '/testUrl' })).toEqual('https://www.example.com/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });

      it('works when the extension URL path contains search parameters', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        expect(ExtensionService.getExtensionUrl({ url: '/testUrl?customParam=X' })).toEqual('https://www.example.com/cms/testUrl?customParam=X&br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });

      it('works when the extension URL path does not start with a slash', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        expect(ExtensionService.getExtensionUrl({ url: 'testUrl' })).toEqual('https://www.example.com/cms/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });

      it('works when the extension URL path contains dots', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        expect(ExtensionService.getExtensionUrl({ url: '../testUrl' })).toEqual('https://www.example.com/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });
    });

    describe('for extensions from a different origin', () => {
      it('works for URLs without parameters', () => {
        expect(ExtensionService.getExtensionUrl({ url: 'http://www.bloomreach.com' })).toEqual('http://www.bloomreach.com/?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });

      it('works for URLs with parameters', () => {
        expect(ExtensionService.getExtensionUrl({ url: 'http://www.bloomreach.com?customParam=X' })).toEqual('http://www.bloomreach.com/?customParam=X&br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });

      it('works for HTTPS URLs', () => {
        expect(ExtensionService.getExtensionUrl({ url: 'https://www.bloomreach.com' })).toEqual('https://www.bloomreach.com/?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443'); // eslint-disable-line max-len
      });
    });
  });
});
