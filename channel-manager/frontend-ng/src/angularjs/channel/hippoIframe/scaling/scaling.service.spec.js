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

describe('ScalingService', () => {
  let $window;
  let ScalingService;
  let ViewportService;
  let hippoIframeElement;
  let canvasElement;
  let iframeElement;
  let iframeHtml;
  let elementsToScale;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_$window_, _ScalingService_, _ViewportService_) => {
      $window = _$window_;
      ScalingService = _ScalingService_;
      ViewportService = _ViewportService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/scaling/scaling.service.fixture.html');

    hippoIframeElement = $('#hippo-iframe');
    canvasElement = $('#channel-iframe-canvas');
    iframeElement = $('#iframe');
  });

  afterEach(() => {
    // prevent subsequent window.resize events by Karma from triggering the scaling service
    $($window).off('resize');
  });

  it('should initialize using the default values', () => {
    expect(ScalingService.getScaleFactor()).toBe(1.0);
    expect(ScalingService.isAnimating()).toBe(false);
  });

  describe('before initialized', () => {
    it('does nothing when setting pushWidth', () => {
      ScalingService.setPushWidth(100);
      expect(ScalingService.getScaleFactor()).toBe(1.0);
    });

    it('does nothing when synced', () => {
      ScalingService.sync();
      expect(ScalingService.getScaleFactor()).toBe(1.0);
    });

    it('does nothing on iframe load or unload', () => {
      expect(() => {
        ScalingService.onIframeUnload();
        ScalingService.onIframeReady();
      }).not.toThrow();
    });
  });

  describe('when initialized', () => {
    beforeEach((done) => {
      ScalingService.init(hippoIframeElement, canvasElement, iframeElement);
      iframeElement.one('load', () => {
        iframeHtml = $('html', iframeElement.contents());
        const appElementToScale = $('.cm-scale');
        elementsToScale = $.merge(iframeHtml, appElementToScale);
        done();
      });
      iframeElement.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/scaling/scaling.service.iframe.fixture.html`);
    });

    it('does not do anything when the hippoIframe is not visible', () => {
      hippoIframeElement.hide();
      ScalingService.setPushWidth(100);
      expect(ScalingService.getScaleFactor()).toBe(1.0);
    });

    function expectTranslateX(elements, px) {
      elements.each(function check() {
        expect($(this).css('transform')).toBe(`matrix(1, 0, 0, 1, ${px}, 0)`);
      });
    }

    function expectScale(elements, factor) {
      elements.each(function check() {
        expect($(this).css('transform')).toBe(`matrix(${factor}, 0, 0, ${factor}, 0, 0)`);
      });
    }

    function expectScaleAndTranslateY(elements, factor, px) {
      elements.each(function check() {
        expect($(this).css('transform')).toBe(`matrix(${factor}, 0, 0, ${factor}, 0, ${px})`);
      });
    }

    it('changes the scaling factor animated when setting pushWidth', () => {
      canvasElement.width(400);

      ScalingService.setPushWidth(100);

      expect(ScalingService.getScaleFactor()).toEqual(0.75);
      expect(iframeElement).toHaveClass('shift-animated');
      expectTranslateX(iframeElement, 0);
      expect(elementsToScale).toHaveClass('hippo-scale-animated');
      expectScaleAndTranslateY(elementsToScale, 0.75, 0);
      expect(ScalingService.isAnimating()).toBe(true);

      elementsToScale.trigger('transitionend');

      expect(ScalingService.isAnimating()).toBe(false);
      expect(elementsToScale).not.toHaveClass('hippo-scale-animated');
      expectScale(elementsToScale, 0.75);
    });

    it('changes the scaling factor instantly when the window is resized', () => {
      canvasElement.width(200);
      ScalingService.setPushWidth(100);
      ScalingService.scaleFactor = 0.75; // fake different scaling factor so the effect of the window resize is testable

      $(window).resize();

      expect(ScalingService.getScaleFactor()).toEqual(0.5);
      expect(elementsToScale).not.toHaveClass('hippo-scale-animated');
      expectScale(elementsToScale, 0.5);
    });

    it('resets the scaling factor animated to 1.0 when setting pushWidth to 0', () => {
      canvasElement.width(400);

      ScalingService.setPushWidth(100, false);
      ScalingService.setPushWidth(0);

      expect(ScalingService.getScaleFactor()).toEqual(1.0);
      expect(iframeElement).toHaveClass('shift-animated');
      expectTranslateX(iframeElement, 0);
      expect(elementsToScale).toHaveClass('hippo-scale-animated');
      expectScaleAndTranslateY(elementsToScale, 1.0, 0);
      expect(ScalingService.isAnimating()).toBe(true);

      elementsToScale.trigger('transitionend');

      expect(ScalingService.isAnimating()).toBe(false);
      expect(elementsToScale).not.toHaveClass('hippo-scale-animated');
      expectScale(elementsToScale, 1.0);
    });

    it('shifts the iframe, unscaled, based on the push width, when the viewport width is smaller than the canvas width', () => {
      canvasElement.width(1280);
      spyOn(ViewportService, 'getWidth').and.returnValue(720);

      ScalingService.setPushWidth(100);

      expect(ScalingService.getScaleFactor()).toEqual(1.0);
      expect(iframeElement).toHaveClass('shift-animated');
      expectTranslateX(iframeElement, 100 / 2);
      expect(elementsToScale).not.toHaveClass('hippo-scale-animated');
      expect(ScalingService.isAnimating()).toBe(false);
    });

    it('scales the iframe when the viewport width is larger than the canvas width', () => {
      canvasElement.width(400);
      spyOn(ViewportService, 'getWidth').and.returnValue(800);

      ScalingService.sync();

      expect(ScalingService.getScaleFactor()).toEqual(0.5);
      expectScale(elementsToScale, 0.5);
      expectTranslateX(iframeElement, -200);
    });

    it('shifts and scales the iframe when the visible canvas gets pushed below the viewport width', () => {
      canvasElement.width(800);
      spyOn(ViewportService, 'getWidth').and.returnValue(720);

      ScalingService.sync();
      ScalingService.setPushWidth(260);

      const expectedShift = (800 - 720) / 2;
      const expectedScaleFactor = (800 - 260) / 720;

      expectTranslateX(iframeElement, expectedShift);
      expectScale(elementsToScale, expectedScaleFactor);
      expect(ScalingService.getScaleFactor()).toEqual(expectedScaleFactor);
    });

    it('unscales a pushed iframe when the viewport width drops below the visible canvas width', () => {
      canvasElement.width(800);

      spyOn(ViewportService, 'getWidth').and.returnValue(720);
      ScalingService.setPushWidth(260);

      // 'resize' the window so the canvas gets a little smaller
      canvasElement.width(720);

      // decrease the viewport size so the viewport now fits in the available canvas
      ViewportService.getWidth.and.returnValue(360);
      ScalingService.sync();

      // validate shifting
      expectTranslateX(iframeElement, 260 / 2);
      expectScale(elementsToScale, 1.0);
      expect(ScalingService.getScaleFactor()).toBe(1.0);
    });

    it('adjusts the vertical scroll position in a scaled iframe', () => {
      const iframeWindow = iframeElement[0].contentWindow;

      // scroll the iframe 80px down
      iframeElement.height(200);
      iframeHtml.height(800);
      iframeWindow.scrollTo(0, 80);

      // scale the iframe
      canvasElement.width(400);
      ScalingService.setPushWidth(100);

      expect(ScalingService.getScaleFactor()).toEqual(0.75);
      expect(iframeElement).toHaveClass('shift-animated');
      expectTranslateX(iframeElement, 0);
      expect(elementsToScale).toHaveClass('hippo-scale-animated');
      expectScaleAndTranslateY(elementsToScale, 0.75, 80 - (0.75 * 80));
      expect(ScalingService.isAnimating()).toBe(true);
      expect(iframeWindow.pageYOffset).toBe(80); // real scroll position is unchanged since we animate via translateY

      elementsToScale.trigger('transitionend');

      expect(ScalingService.isAnimating()).toBe(false);
      expect(elementsToScale).not.toHaveClass('hippo-scale-animated');
      expectScale(elementsToScale, 0.75);
      expect(iframeWindow.pageYOffset).toBe(0.75 * 80);
    });

    describe('when the iframe unloads', () => {
      it('does not hide an unscaled iframe', () => {
        ScalingService.onIframeUnload();
        expect(iframeElement).not.toBeHidden();
      });

      it('hides a scaled iframe', () => {
        // scale the iframe
        canvasElement.width(400);
        spyOn(ViewportService, 'getWidth').and.returnValue(720);
        ScalingService.sync();

        ScalingService.onIframeUnload();

        expect(iframeElement).toBeHidden();
      });
    });

    describe('when the iframe is ready once loaded', () => {
      it('re-applies the current scaling', () => {
        spyOn(iframeElement, 'fadeIn');

        // scale the iframe
        canvasElement.width(400);
        spyOn(ViewportService, 'getWidth').and.returnValue(800);
        ScalingService.sync();
        const scaleFactor = ScalingService.getScaleFactor();

        ScalingService.onIframeReady();

        expect(ScalingService.getScaleFactor()).toEqual(scaleFactor);
        expectScale(elementsToScale, scaleFactor);
        expect(iframeElement.fadeIn).toHaveBeenCalledWith(150);
      });

      it('does not fade in the iframe when it was not scaled', () => {
        spyOn(iframeElement, 'fadeIn');

        ScalingService.onIframeReady();

        expect(ScalingService.getScaleFactor()).toEqual(1.0);
        expect(iframeElement.fadeIn).not.toHaveBeenCalled();
      });
    });
  });
});
