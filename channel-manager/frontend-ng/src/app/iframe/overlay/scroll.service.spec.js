/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

describe('ScrollService', () => {
  let $document;
  let $rootScope;
  let $window;
  let CommunicationService;
  let ScrollService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    jasmine.getFixtures().load('iframe/overlay/scroll.service.fixture.html');

    inject((
      _$document_,
      _$rootScope_,
      _$window_,
      _CommunicationService_,
      _ScrollService_,
    ) => {
      $document = _$document_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      CommunicationService = _CommunicationService_;
      ScrollService = _ScrollService_;
    });

    spyOn(CommunicationService, 'emit');
    spyOn(CommunicationService, 'getScroll');
  });

  describe('getScroll', () => {
    it('should return scroll metrics', () => {
      expect(ScrollService.getScroll()).toEqual(jasmine.objectContaining({
        scrollLeft: jasmine.any(Number),
        scrollTop: jasmine.any(Number),
        scrollWidth: jasmine.any(Number),
        scrollHeight: jasmine.any(Number),
      }));
    });
  });

  describe('setScroll', () => {
    it('should set horizontal scroll', () => {
      ScrollService.setScroll({ scrollLeft: 100 });

      expect($document.find('html, body').scrollLeft()).toBe(100);
    });

    it('should set vertical scroll', () => {
      ScrollService.setScroll({ scrollTop: 100 });

      expect($document.find('html, body').scrollTop()).toBe(100);
    });
  });

  describe('disable', () => {
    beforeEach(() => {
      ScrollService.enable();
      $rootScope.$digest();
      ScrollService.disable();
    });

    it('should stop updating scroll metrics on window resize', () => {
      CommunicationService.getScroll.calls.reset();
      $window.dispatchEvent(new Event('resize'));

      expect(CommunicationService.getScroll).not.toHaveBeenCalled();
    });
  });

  describe('enable', () => {
    beforeEach(() => {
      CommunicationService.getScroll.and.returnValue({
        top: 0,
        right: 100,
        bottom: 100,
        left: 0,
        targetX: 'iframe',
      });
      ScrollService.enable();
      $rootScope.$digest();
    });

    it('should trigger scroll:start event', () => {
      const event = angular.element.Event('mousemove');
      event.pageX = 200;
      event.pageY = 200;

      $document.trigger(event);

      expect(CommunicationService.emit).toHaveBeenCalledWith('scroll:start', { pageX: 200, pageY: 200 });
    });

    it('should trigger scroll:stop event', () => {
      const mouseOut = angular.element.Event('mousemove');
      mouseOut.pageX = 200;
      mouseOut.pageY = 200;

      const mouseIn = angular.element.Event('mousemove');
      mouseIn.pageX = 50;
      mouseIn.pageY = 50;

      CommunicationService.getScroll.calls.reset();
      $document.trigger(mouseOut);
      $document.trigger(mouseIn);

      expect(CommunicationService.emit).toHaveBeenCalledWith('scroll:stop');
      expect(CommunicationService.getScroll).toHaveBeenCalled();
    });

    it('should update scroll metrics on window resize', () => {
      CommunicationService.getScroll.calls.reset();
      $window.dispatchEvent(new Event('resize'));

      expect(CommunicationService.getScroll).toHaveBeenCalled();
    });
  });
});
