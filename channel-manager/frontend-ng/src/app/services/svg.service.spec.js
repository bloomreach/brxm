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
  let $httpBackend;
  let $timeout;
  let $window;
  let SvgService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$httpBackend_, _$timeout_, _$window_, _SvgService_) => {
      $httpBackend = _$httpBackend_;
      $timeout = _$timeout_;
      $window = _$window_;
      SvgService = _SvgService_;
    });
  });

  describe('getSvg', () => {
    beforeEach(() => {
      angular.element($window.document.body)
        .empty();

      $httpBackend.whenGET('sprite.svg')
        .respond(200, '<svg>sprite</svg>');

      SvgService.getSvg($window, {
        url: 'sprite.svg#icon',
        viewBox: '0 0 100 100',
      })
        .wrap('<button>')
        .parent()
        .appendTo($window.document.body);

      $httpBackend.flush();
      $timeout.flush();
    });

    it('injects sprite', () => {
      const sprite = angular.element('body > svg:first', $window.document);

      expect(sprite).toHaveAttr('data-src', 'sprite.svg');
      expect(sprite.text()).toBe('sprite');
    });

    it('places svg icon', () => {
      const icon = angular.element('button svg', $window.document);

      expect(icon).toHaveAttr('width', '100');
      expect(icon).toHaveAttr('height', '100');
      expect(icon[0].getAttribute('viewBox')).toBe('0 0 100 100');
      expect(icon.children('use:first')).toHaveAttr('xlink:href', '#icon');
    });

    it('loads sprite only once', () => {
      SvgService.getSvg($window, {
        url: 'sprite.svg#icon',
        viewBox: '0 0 100 100',
      });

      $timeout.flush();

      $httpBackend.verifyNoOutstandingExpectation();
      expect(angular.element('svg[data-src="sprite.svg"]', $window.document).length).toBe(1);
    });
  });
});
