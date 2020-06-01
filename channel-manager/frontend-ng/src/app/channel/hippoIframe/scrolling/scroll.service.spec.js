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

function range(min, max) {
  return {
    asymmetricMatch(value) {
      return value >= min && value <= max;
    },

    jasmineToString() {
      return `<Range: [${min}, ${max}]>`;
    },
  };
}

describe('ScrollService', () => {
  let BrowserService;
  let CommunicationService;
  let ScrollService;
  let $iframe;
  let $canvas;
  let $sheet;
  let $rootScope;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    jasmine.getFixtures().load('channel/hippoIframe/scrolling/scroll.service.fixture.html');

    BrowserService = jasmine.createSpyObj('BrowserService', ['isFF']);
    CommunicationService = jasmine.createSpyObj('CommunicationService', [
      'disableScroll',
      'enableScroll',
      'getScroll',
      'setScroll',
      'stopScroll',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('BrowserService', BrowserService);
      $provide.value('CommunicationService', CommunicationService);
    });

    $iframe = angular.element('#testIframe');
    $canvas = angular.element('#testCanvas');
    $sheet = angular.element('#testSheet');

    inject((_$rootScope_, _ScrollService_) => {
      $rootScope = _$rootScope_;
      ScrollService = _ScrollService_;
    });

    ScrollService.init($iframe, $canvas, $sheet);
  });

  beforeEach(() => {
    angular.element.fx.off = true;

    CommunicationService.getScroll.and.returnValue({
      scrollLeft: 500,
      scrollTop: 500,

      scrollWidth: 1000,
      scrollHeight: 1000,
    });
  });

  describe('getScroll', () => {
    it('should return scroll metrics', () => {
      expect(ScrollService.getScroll()).toEqual(jasmine.objectContaining({
        top: jasmine.any(Number),
        right: jasmine.any(Number),
        bottom: jasmine.any(Number),
        left: jasmine.any(Number),

        scrollWidth: jasmine.any(Number),
        scrollHeight: jasmine.any(Number),

        scrollLeft: jasmine.any(Number),
        scrollMaxX: jasmine.any(Number),

        targetX: jasmine.any(String),
        targetY: jasmine.any(String),
      }));
    });

    it('should return iframe as a vertical scroll target', () => {
      expect(ScrollService.getScroll()).toEqual(jasmine.objectContaining({ targetY: 'iframe' }));
    });

    it('should return canvas as a horizontal scroll target when the iframe is thiner than canvas', () => {
      $iframe.width(100);

      expect(ScrollService.getScroll()).toEqual(jasmine.objectContaining({ targetX: 'iframe' }));
    });

    it('should return canvas as a horizontal scroll target when the iframe is wider than canvas', () => {
      expect(ScrollService.getScroll()).toEqual(jasmine.objectContaining({ targetX: 'canvas' }));
    });
  });

  describe('disable', () => {
    it('should stop iframe scrolling', () => {
      ScrollService.enable();
      ScrollService.disable();

      expect(CommunicationService.stopScroll).toHaveBeenCalled();
    });

    it('should not stop iframe scrolling if it was not enabled previously', () => {
      ScrollService.disable();

      expect(CommunicationService.stopScroll).not.toHaveBeenCalled();
    });

    it('should stop reacting on mouseleave events', () => {
      ScrollService.enable();
      ScrollService.disable();

      const event = angular.element.Event('mouseleave');
      event.pageX = 300;
      event.pageY = 100;

      $iframe.width(100);
      $iframe.trigger(event);
      $rootScope.$digest();

      expect(CommunicationService.setScroll).not.toHaveBeenCalled();
    });

    it('should stop reacting on mouseenter events', () => {
      ScrollService.enable();
      ScrollService.disable();

      CommunicationService.stopScroll.calls.reset();

      const event = angular.element.Event('mouseenter');

      $iframe.width(100);
      $iframe.trigger(event);
      $rootScope.$digest();

      expect(CommunicationService.stopScroll).not.toHaveBeenCalled();
    });

    it('should disable iframe scrolling in firefox', () => {
      BrowserService.isFF.and.returnValue(true);
      ScrollService.enable();
      ScrollService.disable();

      expect(CommunicationService.disableScroll).toHaveBeenCalled();
    });

    it('should stop reacting on scroll:start events in firefox', () => {
      BrowserService.isFF.and.returnValue(true);
      ScrollService.enable();
      ScrollService.disable();

      $iframe.width(100);
      $rootScope.$emit('iframe:scroll:start', { pageX: 300, pageY: 100 });
      $rootScope.$digest();

      expect(CommunicationService.setScroll).not.toHaveBeenCalled();
    });

    it('should stop reacting on scroll:stop events in firefox', () => {
      BrowserService.isFF.and.returnValue(true);
      ScrollService.enable();
      ScrollService.disable();

      CommunicationService.stopScroll.calls.reset();

      $iframe.width(100);
      $rootScope.$emit('iframe:scroll:stop');
      $rootScope.$digest();

      expect(CommunicationService.stopScroll).not.toHaveBeenCalled();
    });
  });

  describe('enable', () => {
    describe('scrolling in all browsers', () => {
      beforeEach(() => {
        ScrollService.enable();
      });

      it('should scroll canvas left', () => {
        const event = angular.element.Event('mouseleave');
        event.pageX = 0;
        event.pageY = 100;

        $canvas.scrollLeft(50);
        $iframe.trigger(event);
        $rootScope.$digest();

        expect($canvas.scrollLeft()).toBe(0);
      });

      it('should scroll canvas right', () => {
        const event = angular.element.Event('mouseleave');
        event.pageX = 300;
        event.pageY = 100;

        $iframe.trigger(event);
        $rootScope.$digest();

        expect($canvas.scrollLeft()).toBe(200);
      });

      it('should scroll iframe left', () => {
        const event = angular.element.Event('mouseleave');
        event.pageX = 0;
        event.pageY = 100;

        $iframe.width(100);
        $iframe.trigger(event);
        $rootScope.$digest();

        expect(CommunicationService.setScroll).toHaveBeenCalledWith({ scrollLeft: 0 }, 1000);
      });

      it('should scroll iframe right', () => {
        const event = angular.element.Event('mouseleave');
        event.pageX = 300;
        event.pageY = 100;

        $iframe.width(100);

        $iframe.trigger(event);
        $rootScope.$digest();

        // add a scrollbar error
        expect(CommunicationService.setScroll).toHaveBeenCalledWith({ scrollLeft: range(900, 950) }, range(800, 850));
      });

      it('should scroll iframe up', () => {
        const event = angular.element.Event('mouseleave');
        event.pageX = 100;
        event.pageY = 0;

        $iframe.height(100);
        $iframe.trigger(event);
        $rootScope.$digest();

        expect(CommunicationService.setScroll).toHaveBeenCalledWith({ scrollTop: 0 }, 1000);
      });

      it('should scroll iframe down', () => {
        const event = angular.element.Event('mouseleave');
        event.pageX = 100;
        event.pageY = 300;

        $iframe.height(100);
        $iframe.trigger(event);
        $rootScope.$digest();

        // add a scrollbar error
        expect(CommunicationService.setScroll).toHaveBeenCalledWith({ scrollTop: range(900, 950) }, range(800, 850));
      });

      it('should not scroll iframe if it is already there', () => {
        CommunicationService.getScroll.and.returnValue({
          scrollLeft: 0,
          scrollTop: 0,

          scrollWidth: 1000,
          scrollHeight: 1000,
        });

        const event = angular.element.Event('mouseleave');
        event.pageX = 0;
        event.pageY = 100;

        $iframe.width(100);
        $iframe.trigger(event);
        $rootScope.$digest();

        expect(CommunicationService.setScroll).not.toHaveBeenCalled();
      });

      it('should stop iframe scrolling on mouseenter event', () => {
        const event = angular.element.Event('mouseenter');

        $iframe.trigger(event);
        $rootScope.$digest();

        expect(CommunicationService.stopScroll).toHaveBeenCalled();
      });
    });

    describe('scrolling in firefox', () => {
      beforeEach(() => {
        BrowserService.isFF.and.returnValue(true);
        ScrollService.enable();
      });

      it('should enable scroll inside the iframe', () => {
        expect(CommunicationService.enableScroll).toHaveBeenCalled();
      });

      it('should scroll on scroll:start event', () => {
        $iframe.width(100);
        $rootScope.$emit('iframe:scroll:start', { pageX: 0, pageY: 100 });
        $rootScope.$digest();

        expect(CommunicationService.setScroll).toHaveBeenCalledWith({ scrollLeft: 0 }, 1000);
      });

      it('should stop scroll on scroll:stop event', () => {
        $iframe.width(100);
        $rootScope.$emit('iframe:scroll:stop');
        $rootScope.$digest();

        expect(CommunicationService.stopScroll).toHaveBeenCalled();
      });
    });
  });

  describe('savePosition', () => {
    it('should get iframe scroll metrics', () => {
      ScrollService.savePosition();

      expect(CommunicationService.getScroll).toHaveBeenCalled();
    });
  });

  describe('restorePosition', () => {
    it('should restore iframe scroll', () => {
      CommunicationService.getScroll.and.returnValue({
        scrollLeft: 100,
        scrollTop: 200,
      });

      ScrollService.savePosition();
      $rootScope.$digest();
      ScrollService.restorePosition();

      expect(CommunicationService.setScroll).toHaveBeenCalledWith({ scrollLeft: 100, scrollTop: 200 });
    });

    it('should restore canvas scroll', () => {
      $canvas.scrollLeft(200);

      ScrollService.savePosition();
      $rootScope.$digest();

      spyOn($canvas, 'scrollLeft');
      ScrollService.restorePosition();

      expect($canvas.scrollLeft).toHaveBeenCalledWith(200);
    });

    it('should not restore any scroll position if it was previously restored', () => {
      ScrollService.savePosition();
      $rootScope.$digest();
      ScrollService.restorePosition();

      spyOn($canvas, 'scrollLeft');
      CommunicationService.setScroll.calls.reset();
      ScrollService.restorePosition();

      expect($canvas.scrollLeft).not.toHaveBeenCalled();
      expect(CommunicationService.setScroll).not.toHaveBeenCalled();
    });
  });
});
