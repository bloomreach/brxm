/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
  let ScrollService;
  let $iframe;
  const iframeSrc = `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/scrolling/scroll.service.iframe.fixture.html`;

  const iframeTop = 40;
  const iframeBottom = 240;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    jasmine.getFixtures().load('channel/hippoIframe/scrolling/scroll.service.fixture.html');
    $iframe = $('#testIframe');

    inject((_ScrollService_) => {
      ScrollService = _ScrollService_;
      ScrollService.init($iframe);
    });
  });

  function loadIframeFixture(callback) {
    $iframe.one('load', () => {
      try {
        callback();
      } catch (e) {
        fail(e);
      }
    });
    $iframe.attr('src', iframeSrc);
    ScrollService.init($iframe);
  }

  it('should be possible to configure the type of easing', () => {
    ScrollService.init(null, 'custom-easing');
    expect(ScrollService.easing).toEqual('custom-easing');
  });

  it('should start/stop scrolling on mouse enter/leave events', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, 'startScrolling').and.callThrough();
      spyOn(ScrollService, 'stopScrolling').and.callThrough();
      ScrollService.enable();

      $iframe.trigger('mouseleave');
      expect(ScrollService.startScrolling).toHaveBeenCalled();

      $iframe.trigger('mouseenter');
      expect(ScrollService.stopScrolling).toHaveBeenCalled();

      done();
    });
  });

  it('should only attach mouse-enter/mouse-leave once', (done) => {
    loadIframeFixture(() => {
      spyOn($iframe, 'on').and.callThrough();
      ScrollService.enable();
      expect($iframe.on.calls.count()).toEqual(2);
      $iframe.on.calls.reset();

      ScrollService.enable();
      expect($iframe.on).not.toHaveBeenCalled();
      done();
    });
  });

  it('should not start scrolling when the shouldScroll argument returns false', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, 'startScrolling');
      ScrollService.enable(() => false);

      $iframe.trigger('mouseleave');
      expect(ScrollService.startScrolling).not.toHaveBeenCalled();
      done();
    });
  });

  it('should stop scrolling and remove event listeners when disabled', (done) => {
    loadIframeFixture(() => {
      spyOn($iframe, 'off').and.callThrough();
      spyOn(ScrollService, 'stopScrolling');

      ScrollService.enable();
      ScrollService.disable();

      expect($iframe.off).toHaveBeenCalled();
      expect(ScrollService.stopScrolling).toHaveBeenCalled();
      done();
    });
  });

  it('should only stop scrolling and remove event listeners when disabled if previously enabled', (done) => {
    loadIframeFixture(() => {
      spyOn($iframe, 'off').and.callThrough();
      spyOn(ScrollService, 'stopScrolling');

      ScrollService.disable();

      expect($iframe.off).not.toHaveBeenCalled();
      expect(ScrollService.stopScrolling).not.toHaveBeenCalled();
      done();
    });
  });

  it('should not scroll up when page is at the top and mouse leaves on top', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      ScrollService.startScrolling(null, 10);

      expect(ScrollService._scroll).not.toHaveBeenCalled();
      done();
    });
  });

  it('should scroll up when page is not at the top and mouse leaves at the top', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(100);

      ScrollService.enable();
      ScrollService.startScrolling(null, iframeTop);

      expect(ScrollService._scroll).toHaveBeenCalledWith(0, jasmine.any(Number));
      done();
    });
  });

  it('should scroll up when page is at the bottom and mouse leaves at the top', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(200);

      ScrollService.enable();
      ScrollService.startScrolling(null, iframeTop);

      expect(ScrollService._scroll).toHaveBeenCalledWith(0, jasmine.any(Number));
      done();
    });
  });

  it('should not scroll down when page is at the bottom and mouse leaves at the bottom', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(200);

      ScrollService.enable();
      ScrollService.startScrolling(null, iframeBottom);

      expect(ScrollService._scroll).not.toHaveBeenCalled();
      done();
    });
  });

  it('should scroll down when page is at the top and mouse leaves at the bottom', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      ScrollService.startScrolling(null, iframeBottom);

      expect(ScrollService._scroll).toHaveBeenCalledWith(200, jasmine.any(Number));
      done();
    });
  });

  it('should scroll down when page is not at the bottom and mouse leaves at the bottom', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(50);

      ScrollService.enable();
      ScrollService.startScrolling(null, iframeBottom);

      expect(ScrollService._scroll).toHaveBeenCalledWith(200, jasmine.any(Number));
      done();
    });
  });

  it('should always start a new scroll animation', (done) => {
    loadIframeFixture(() => {
      const scrollable = {};
      scrollable.stop = jasmine.createSpy().and.returnValue(scrollable);
      scrollable.animate = jasmine.createSpy().and.returnValue(scrollable);
      ScrollService.scrollable = scrollable;

      ScrollService._scroll(0, 0);
      expect(scrollable.stop).toHaveBeenCalled();
      expect(scrollable.animate).toHaveBeenCalledWith({ scrollTop: 0 }, { duration: 0, easing: 'swing' });
      done();
    });
  });

  it('should calculate a scroll duration based on distance between a min and max time', () => {
    expect(ScrollService._calculateDuration(0)).toBe(0);
    expect(ScrollService._calculateDuration(0.5, 500, 1500)).toBe(500);
    expect(ScrollService._calculateDuration(250, 500, 1500)).toBe(500);
    expect(ScrollService._calculateDuration(300, 500, 1500)).toBe(600);
    expect(ScrollService._calculateDuration(750, 500, 1500)).toBe(1500);
    expect(ScrollService._calculateDuration(800, 500, 1500)).toBe(1500);
  });
});
