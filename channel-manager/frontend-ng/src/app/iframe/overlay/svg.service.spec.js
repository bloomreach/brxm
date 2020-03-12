/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
  let $httpBackend;
  let $q;
  let $timeout;
  let CommunicationService;
  let SvgService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['getAssetUrl']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
    });

    inject((_$document_, _$httpBackend_, _$q_, _$timeout_, _SvgService_) => {
      $document = _$document_;
      $httpBackend = _$httpBackend_;
      $q = _$q_;
      $timeout = _$timeout_;
      SvgService = _SvgService_;
    });

    CommunicationService.getAssetUrl.and.callFake(href => $q.resolve(href));
  });

  describe('getSvg', () => {
    beforeEach(() => {
      $document.find('body').empty();

      $httpBackend.whenGET('sprite.svg')
        .respond(200, '<svg>sprite</svg>');

      SvgService.getSvg({
        url: 'sprite.svg#icon',
        viewBox: '0 0 100 100',
      })
        .wrap('<button>')
        .parent()
        .appendTo($document.find('body'));

      $httpBackend.flush();
      $timeout.flush();
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

      $timeout.flush();

      $httpBackend.verifyNoOutstandingExpectation();
      expect($document.find('svg[data-src="sprite.svg"]')).toHaveLength(1);
    });
  });
});
