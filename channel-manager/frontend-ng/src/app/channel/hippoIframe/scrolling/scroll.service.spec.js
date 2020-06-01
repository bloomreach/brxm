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

describe('ScrollService', () => {
  let ScrollService;
  let $iframe;
  let $canvas;
  let $sheet;
  let maxScrollTop;
  let maxScrollLeft;

  const margin = 10; // this margin ensures the mouse is over the element by 10 px
  const offsetLeft = 40;
  const offsetTop = 40;
  const canvasHeight = 200;
  const canvasWidth = 200;
  const iframeHeight = 400;
  const iframeWidth = 400;

  const exitOnTop = [margin + offsetLeft, offsetTop];
  const exitOnBottom = [margin + offsetLeft, canvasHeight + offsetTop];
  const exitOnLeft = [offsetLeft, margin + offsetTop];
  const exitOnRight = [canvasWidth + offsetLeft, margin + offsetTop];

  const iframeSrc = `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/scrolling/scroll.service.iframe.fixture.html`; // eslint-disable-line max-len

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    jasmine.getFixtures().load('channel/hippoIframe/scrolling/scroll.service.fixture.html');
    $iframe = $('#testIframe');
    $canvas = $('#testCanvas');
    $sheet = $('#testSheet');

    inject((_ScrollService_) => {
      ScrollService = _ScrollService_;
      ScrollService.init($iframe, $canvas, $sheet);
    });
  });

  beforeEach(() => {
    const scrollBarSize = ScrollService.getScrollBarSize();
    maxScrollTop = (iframeHeight - canvasHeight) + scrollBarSize;
    maxScrollLeft = iframeWidth - canvasWidth;
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
    ScrollService.init($iframe, $canvas, $sheet);
  }

  describe('mouse enter/leave event listeners', () => {
    it('should start/stop scrolling on mouse enter/leave events', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling').and.callThrough();
        spyOn(ScrollService, '_stopScrolling').and.callThrough();
        ScrollService.enable(() => true);

        $iframe.trigger('mouseleave');
        expect(ScrollService._startScrolling).toHaveBeenCalled();

        $iframe.trigger('mouseenter');
        expect(ScrollService._stopScrolling).toHaveBeenCalled();

        done();
      });
    });

    it('should only attach mouse-enter/mouse-leave once', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_bindMouseEnterMouseLeave');
        ScrollService.enable();
        expect(ScrollService._bindMouseEnterMouseLeave).toHaveBeenCalled();
        ScrollService._bindMouseEnterMouseLeave.calls.reset();

        ScrollService.enable();
        expect(ScrollService._bindMouseEnterMouseLeave).not.toHaveBeenCalled();
        done();
      });
    });

    it('should stop scrolling and unbind event listeners when disabled', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_unbindMouseEnterMouseLeave').and.callThrough();
        spyOn(ScrollService, '_stopScrolling');

        ScrollService.enable();
        ScrollService.disable();

        expect(ScrollService._unbindMouseEnterMouseLeave).toHaveBeenCalled();
        expect(ScrollService._stopScrolling).toHaveBeenCalled();
        done();
      });
    });

    it('should only stop scrolling and unbind event listeners if previously enabled', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_unbindMouseEnterMouseLeave');
        spyOn(ScrollService, '_stopScrolling');

        ScrollService.disable();

        expect(ScrollService._unbindMouseEnterMouseLeave).not.toHaveBeenCalled();
        expect(ScrollService._stopScrolling).not.toHaveBeenCalled();
        done();
      });
    });
  });

  describe('scroll up', () => {
    it('should not scroll up when page is at the top and mouse leaves on top', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        ScrollService.enable();
        ScrollService._startScrolling(...exitOnTop);

        expect(ScrollService._scroll).not.toHaveBeenCalled();
        done();
      });
    });

    it('should scroll up when page is not at the top and mouse leaves at the top', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        const body = $iframe.contents().find('body');
        body.scrollTop(maxScrollTop / 2);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnTop);

        const htmlBody = $iframe.contents().find('html, body');
        expect(ScrollService._scroll).toHaveBeenCalledWith(htmlBody, { scrollTop: 0 }, jasmine.any(Number));
        done();
      });
    });

    it('should scroll up when page is at the bottom and mouse leaves at the top', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        const body = $iframe.contents().find('body');
        body.scrollTop(maxScrollTop);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnTop);

        const htmlBody = $iframe.contents().find('html, body');
        expect(ScrollService._scroll).toHaveBeenCalledWith(htmlBody, { scrollTop: 0 }, jasmine.any(Number));
        done();
      });
    });
  });

  describe('scroll down', () => {
    it('should not scroll down when page is at the bottom and mouse leaves at the bottom', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        const body = $iframe.contents().find('body');
        body.scrollTop(maxScrollTop);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnBottom);

        expect(ScrollService._scroll).not.toHaveBeenCalled();
        done();
      });
    });

    it('should scroll down when page is not at the bottom and mouse leaves at the bottom', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        const body = $iframe.contents().find('body');
        body.scrollTop(maxScrollTop / 2);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnBottom);

        const htmlBody = $iframe.contents().find('html, body');
        expect(ScrollService._scroll).toHaveBeenCalledWith(htmlBody, { scrollTop: maxScrollTop }, jasmine.any(Number));
        done();
      });
    });

    it('should scroll down when page is at the top and mouse leaves at the bottom', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        ScrollService.enable();
        ScrollService._startScrolling(...exitOnBottom);

        const htmlBody = $iframe.contents().find('html, body');
        expect(ScrollService._scroll).toHaveBeenCalledWith(htmlBody, { scrollTop: maxScrollTop }, jasmine.any(Number));
        done();
      });
    });
  });

  describe('scroll left', () => {
    it('should not scroll left when page is at the left and mouse leaves at the left', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        ScrollService.enable();
        ScrollService._startScrolling(...exitOnLeft);

        expect(ScrollService._scroll).not.toHaveBeenCalled();
        done();
      });
    });

    it('should scroll left when page is not at the left and mouse leaves at the left', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        $canvas.scrollLeft(maxScrollLeft / 2);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnLeft);

        expect(ScrollService._scroll).toHaveBeenCalledWith($canvas, { scrollLeft: 0 }, jasmine.any(Number));
        done();
      });
    });

    it('should scroll left when page is at the right and mouse leaves at the left', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        $canvas.scrollLeft(maxScrollLeft);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnLeft);

        expect(ScrollService._scroll).toHaveBeenCalledWith($canvas, { scrollLeft: 0 }, jasmine.any(Number));
        done();
      });
    });
  });

  describe('scroll right', () => {
    it('should not scroll right when page is at the right and mouse leaves at the right', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        $canvas.scrollLeft(maxScrollLeft);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnRight);

        expect(ScrollService._scroll).not.toHaveBeenCalled();
        done();
      });
    });

    it('should scroll right when page is not at the right and mouse leaves at the right', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        $canvas.scrollLeft(maxScrollLeft / 2);
        ScrollService.enable();
        ScrollService._startScrolling(...exitOnRight);

        expect(ScrollService._scroll).toHaveBeenCalledWith($canvas, { scrollLeft: maxScrollLeft }, jasmine.any(Number));
        done();
      });
    });

    it('should scroll right when page is at the left and mouse leaves at the right', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_scroll');

        ScrollService.enable();
        ScrollService._startScrolling(...exitOnRight);

        expect(ScrollService._scroll).toHaveBeenCalledWith($canvas, { scrollLeft: maxScrollLeft }, jasmine.any(Number));
        done();
      });
    });
  });

  describe('start/stop of scroll animation', () => {
    it('should always start a new horizontal scroll animation', (done) => {
      loadIframeFixture(() => {
        ScrollService.enable();
        spyOn(ScrollService.canvas, 'stop').and.callThrough();
        spyOn(ScrollService.canvas, 'animate').and.callThrough();

        const to = { scrollLeft: 0 };
        ScrollService._scroll(ScrollService.canvas, to, 0);

        expect(ScrollService.canvas.stop).toHaveBeenCalled();
        expect(ScrollService.canvas.animate).toHaveBeenCalledWith(to, { duration: 0 });
        done();
      });
    });

    it('should always start a new vertical scroll animation', (done) => {
      loadIframeFixture(() => {
        ScrollService.enable();
        spyOn(ScrollService.iframeHtmlBody, 'stop').and.callThrough();
        spyOn(ScrollService.iframeHtmlBody, 'animate').and.callThrough();

        const to = { scrollTop: 0 };
        ScrollService._scroll(ScrollService.iframeHtmlBody, to, 0);

        expect(ScrollService.iframeHtmlBody.stop).toHaveBeenCalled();
        expect(ScrollService.iframeHtmlBody.animate).toHaveBeenCalledWith(to, { duration: 0 });
        done();
      });
    });

    it('should stop the horizontal scrolling animation when stopScrolling is called', (done) => {
      loadIframeFixture(() => {
        ScrollService.enable();
        const canvas = spyOn(ScrollService.canvas, 'stop');

        ScrollService._stopScrolling();

        expect(canvas).toHaveBeenCalled();
        canvas.calls.reset();

        ScrollService.canvas = null;
        ScrollService._stopScrolling();

        expect(canvas).not.toHaveBeenCalled();
        done();
      });
    });

    it('should stop the vertical scrolling animation when stopScrolling is called', (done) => {
      loadIframeFixture(() => {
        ScrollService.enable();
        const iframeHtmlBody = spyOn(ScrollService.iframeHtmlBody, 'stop');

        ScrollService._stopScrolling();

        expect(iframeHtmlBody).toHaveBeenCalled();
        iframeHtmlBody.calls.reset();

        ScrollService.iframeHtmlBody = null;
        ScrollService._stopScrolling();

        expect(iframeHtmlBody).not.toHaveBeenCalled();
        done();
      });
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

  describe('save/restore scroll position', () => {
    it('should save scroll position on canvas', (done) => {
      loadIframeFixture(() => {
        const iframeWindow = $($iframe[0].contentWindow);

        $iframe.width(400);
        $canvas.width(200);
        iframeWindow.scrollTop(10);
        $canvas.scrollLeft(20);

        ScrollService.saveScrollPosition();
        expect(ScrollService.savedScrollPosition).toEqual({
          top: 10,
          canvasLeft: 20,
          iframeLeft: 0,
        });

        done();
      });
    });

    it('should save scroll position on iframe', (done) => {
      loadIframeFixture(() => {
        const iframeWindow = $($iframe[0].contentWindow);

        $iframe.width(400);
        iframeWindow.scrollTop(10);
        $iframe.contents().find('.channel-iframe-element').width(600);
        iframeWindow.scrollLeft(30);

        ScrollService.saveScrollPosition();
        expect(ScrollService.savedScrollPosition).toEqual({
          top: 10,
          iframeLeft: 30,
          canvasLeft: 0,
        });

        done();
      });
    });

    it('should restore scroll position on canvas', (done) => {
      loadIframeFixture(() => {
        const iframeWindow = $($iframe[0].contentWindow);
        $iframe.width(400);
        $canvas.width(200);

        ScrollService.savedScrollPosition = {
          top: 30,
          iframeLeft: 0,
          canvasLeft: 20,
        };
        ScrollService.restoreScrollPosition();

        expect(iframeWindow.scrollTop()).toEqual(30);
        expect(iframeWindow.scrollLeft()).toEqual(0);
        expect($canvas.scrollLeft()).toEqual(20);

        done();
      });
    });

    it('should restore scroll position on iframe', (done) => {
      loadIframeFixture(() => {
        const iframeWindow = $($iframe[0].contentWindow);

        $iframe.width(400);
        $iframe.contents().find('.channel-iframe-element').width(600);
        iframeWindow.scrollLeft(30);

        ScrollService.savedScrollPosition = {
          top: 15,
          iframeLeft: 10,
          canvasLeft: 0,
        };
        ScrollService.restoreScrollPosition();

        expect(iframeWindow.scrollTop()).toEqual(15);
        expect(iframeWindow.scrollLeft()).toEqual(10);
        expect($canvas.scrollLeft()).toEqual(0);

        done();
      });
    });
  });

  describe('the mousemove workaround for Firefox', () => {
    let BrowserService;

    beforeEach(() => {
      inject((_BrowserService_) => {
        BrowserService = _BrowserService_;
        spyOn(BrowserService, 'isFF').and.returnValue(true);
      });
    });

    function triggerMouseMove(pageX, pageY) {
      const event = $.Event('mousemove'); // eslint-disable-line new-cap
      event.pageX = pageX;
      event.pageY = pageY;
      $iframe.contents().trigger(event);
    }

    it('should bind the mousemove event handler when the service is enabled', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_bindMouseMove');

        ScrollService.enable();
        expect(ScrollService._bindMouseMove).toHaveBeenCalled();

        done();
      });
    });

    it('should unbind the mousemove event handler when the service is disabled', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_unbindMouseMove').and.callThrough();

        ScrollService.enable();
        ScrollService.disable();

        expect(ScrollService._unbindMouseMove).toHaveBeenCalled();
        done();
      });
    });

    it('should call start scrolling once when mouse moves over the upper bound', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(1, 0);
        expect(ScrollService._startScrolling).toHaveBeenCalled();
        ScrollService._startScrolling.calls.reset();

        triggerMouseMove(1, 0);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();

        done();
      });
    });

    it('should call start scrolling once when mouse moves over the lower bound', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(1, canvasHeight);
        expect(ScrollService._startScrolling).toHaveBeenCalled();
        ScrollService._startScrolling.calls.reset();

        triggerMouseMove(1, canvasHeight);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();

        done();
      });
    });

    it('should call start scrolling once when mouse moves over the left bound', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(0, 1);
        expect(ScrollService._startScrolling).toHaveBeenCalled();
        ScrollService._startScrolling.calls.reset();

        triggerMouseMove(0, 1);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();

        done();
      });
    });

    it('should call start scrolling once when mouse moves over the right bound', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(canvasWidth, 1);
        expect(ScrollService._startScrolling).toHaveBeenCalled();
        ScrollService._startScrolling.calls.reset();

        triggerMouseMove(canvasWidth, 1);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();

        done();
      });
    });

    it('should call stop scrolling once when mouse re-enters the page', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_stopScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(0, 0);
        triggerMouseMove(10, 10);
        expect(ScrollService._stopScrolling).toHaveBeenCalled();
        ScrollService._stopScrolling.calls.reset();

        triggerMouseMove(11, 11);
        expect(ScrollService._stopScrolling).not.toHaveBeenCalled();

        done();
      });
    });
  });

  it('should calculate the scroll bar width', () => {
    const size = ScrollService.getScrollBarSize();
    expect(size).toBeGreaterThan(-1);
  });
});
