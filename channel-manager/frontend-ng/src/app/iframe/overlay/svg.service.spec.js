/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('SvgService', () => {
  let $document;
  let $q;
  let $rootScope;
  let CommunicationService;
  let SvgService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['getAsset']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
    });

    inject((_$document_, _$q_, _$rootScope_, _SvgService_) => {
      $document = _$document_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      SvgService = _SvgService_;
    });
  });

  describe('getSvg', () => {
    beforeEach(() => {
      $document.find('body').empty();

      CommunicationService.getAsset.and.returnValue($q.resolve('<svg>sprite</svg>'));

      SvgService.getSvg({
        url: 'sprite.svg#icon',
        viewBox: '0 0 100 100',
      })
        .wrap('<button>')
        .parent()
        .appendTo($document.find('body'));

      $rootScope.$digest();
    });

    it('injects sprite', () => {
      const sprite = $document.find('body > svg:first');

      expect(sprite).toHaveAttr('data-src', 'sprite.svg');
      expect(sprite.text()).toBe('sprite');
    });

    it('places svg icon', () => {
      const icon = $document.find('button svg');

      expect(icon).toHaveAttr('width', '100');
      expect(icon).toHaveAttr('height', '100');
      expect(icon[0].getAttribute('viewBox')).toBe('0 0 100 100');
      expect(icon.children('use:first')).toHaveAttr('xlink:href', '#icon');
    });

    it('loads sprite only once', () => {
      SvgService.getSvg({
        url: 'sprite.svg#icon',
        viewBox: '0 0 100 100',
      });
      $rootScope.$digest();

      expect($document.find('svg[data-src="sprite.svg"]')).toHaveLength(1);
    });
  });
});
