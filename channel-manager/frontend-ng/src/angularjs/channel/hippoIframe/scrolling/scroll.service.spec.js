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

  it('should not start scrolling when the scrollAllowed argument returns false', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_startScrolling');
      ScrollService.enable(() => false);

      $iframe.trigger('mouseleave');
      expect(ScrollService._startScrolling).not.toHaveBeenCalled();
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

  it('should not scroll up when page is at the top and mouse leaves on top', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      ScrollService._startScrolling(0, 10);

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
      ScrollService._startScrolling(0, iframeTop);

      expect(ScrollService._scroll).toHaveBeenCalledWith({ scrollTop: 0 }, jasmine.any(Number));
      done();
    });
  });

  it('should scroll up when page is at the bottom and mouse leaves at the top', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(200);

      ScrollService.enable();
      ScrollService._startScrolling(0, iframeTop);

      expect(ScrollService._scroll).toHaveBeenCalledWith({ scrollTop: 0 }, jasmine.any(Number));
      done();
    });
  });

  it('should not scroll down when page is at the bottom and mouse leaves at the bottom', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(200);

      ScrollService.enable();
      ScrollService._startScrolling(0, iframeBottom);

      expect(ScrollService._scroll).not.toHaveBeenCalled();
      done();
    });
  });

  it('should scroll down when page is at the top and mouse leaves at the bottom', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      ScrollService._startScrolling(0, iframeBottom);

      expect(ScrollService._scroll).toHaveBeenCalledWith({ scrollTop: 200 }, jasmine.any(Number));
      done();
    });
  });

  it('should scroll down when page is not at the bottom and mouse leaves at the bottom', (done) => {
    loadIframeFixture(() => {
      spyOn(ScrollService, '_scroll');

      const body = $iframe.contents().find('body');
      body.scrollTop(50);

      ScrollService.enable();
      ScrollService._startScrolling(0, iframeBottom);

      expect(ScrollService._scroll).toHaveBeenCalledWith({ scrollTop: 200 }, jasmine.any(Number));
      done();
    });
  });

  it('should always start a new scroll animation', (done) => {
    loadIframeFixture(() => {
      ScrollService.enable();
      spyOn(ScrollService.iframeHtmlBody, 'stop').and.callThrough();
      spyOn(ScrollService.iframeHtmlBody, 'animate').and.callThrough();

      const to = { scrollTop: 0 };
      ScrollService._scroll(to, 0);

      expect(ScrollService.iframeHtmlBody.stop).toHaveBeenCalled();
      expect(ScrollService.iframeHtmlBody.animate).toHaveBeenCalledWith(to, { duration: 0 });
      done();
    });
  });

  it('should stop the animation when stopscrolling is called', (done) => {
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

  it('should calculate a scroll duration based on distance between a min and max time', () => {
    expect(ScrollService._calculateDuration(0)).toBe(0);
    expect(ScrollService._calculateDuration(0.5, 500, 1500)).toBe(500);
    expect(ScrollService._calculateDuration(250, 500, 1500)).toBe(500);
    expect(ScrollService._calculateDuration(300, 500, 1500)).toBe(600);
    expect(ScrollService._calculateDuration(750, 500, 1500)).toBe(1500);
    expect(ScrollService._calculateDuration(800, 500, 1500)).toBe(1500);
  });

  it('should save scroll position', (done) => {
    loadIframeFixture(() => {
      const iframeDocument = $iframe.contents();
      const iframeHtmlBody = iframeDocument.find('html,body');

      iframeHtmlBody.scrollTop(50);
      iframeHtmlBody.scrollLeft(60);

      ScrollService.saveScrollPosition();

      expect(ScrollService.savedScrollPosition).toEqual({
        top: 50,
        left: 60,
      });
      done();
    });
  });

  it('should restore scroll position', (done) => {
    loadIframeFixture(() => {
      ScrollService.savedScrollPosition = {
        top: 20,
        left: 30,
      };

      ScrollService.restoreScrollPosition();

      const iframeWindow = $($iframe[0].contentWindow);
      expect(iframeWindow.scrollTop()).toEqual(20);
      expect(iframeWindow.scrollLeft()).toEqual(30);
      done();
    });
  });

  describe('the workaround for Firefox', () => {
    let BrowserService;

    beforeEach(() => {
      inject((_BrowserService_) => {
        BrowserService = _BrowserService_;
        spyOn(BrowserService, 'isFF').and.returnValue(true);
      });
    });

    function triggerMouseMove(pageY) {
      const event = $.Event('mousemove'); // eslint-disable-line new-cap
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

    it('should not start scrolling when the scrollAllowed argument returns false', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => false);

        triggerMouseMove(0);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();
        done();
      });
    });

    it('should call start scrolling once when mouse moves over the upper bound', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(0);
        expect(ScrollService._startScrolling).toHaveBeenCalled();
        ScrollService._startScrolling.calls.reset();

        triggerMouseMove(0);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();

        done();
      });
    });

    it('should call start scrolling once when mouse moves over the lower bound', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_startScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(200);
        expect(ScrollService._startScrolling).toHaveBeenCalled();
        ScrollService._startScrolling.calls.reset();

        triggerMouseMove(200);
        expect(ScrollService._startScrolling).not.toHaveBeenCalled();

        done();
      });
    });

    it('should call stop scrolling once when mouse re-enters the page', (done) => {
      loadIframeFixture(() => {
        spyOn(ScrollService, '_stopScrolling');
        ScrollService.enable(() => true);

        triggerMouseMove(0);
        triggerMouseMove(10);
        expect(ScrollService._stopScrolling).toHaveBeenCalled();
        ScrollService._stopScrolling.calls.reset();

        triggerMouseMove(11);
        expect(ScrollService._stopScrolling).not.toHaveBeenCalled();

        done();
      });
    });
  });
});
