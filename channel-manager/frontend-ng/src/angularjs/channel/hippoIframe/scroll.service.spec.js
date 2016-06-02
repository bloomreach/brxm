/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('ScrollService', () => {
  'use strict';

  let ScrollService;
  let baseJQueryElement;
  let velocitySpy;

  beforeEach(() => {
    module('hippo-cm.channel.hippoIframe');

    jasmine.getFixtures().load('channel/hippoIframe/scroll.service.fixture.html');

    baseJQueryElement = $j('.channel-iframe-base');
    const iframeJQueryElement = $j('.channel-iframe-element');
    iframeJQueryElement.velocity = () => {};
    velocitySpy = spyOn(iframeJQueryElement, 'velocity').and.returnValue(iframeJQueryElement);

    inject((_ScrollService_) => {
      ScrollService = _ScrollService_;
      ScrollService.init(iframeJQueryElement, baseJQueryElement);
    });
  });

  it('should be possible to configure the type of easing', () => {
    ScrollService.init(null, null, 'custom-easing');
    expect(ScrollService.easing).toEqual('custom-easing');
  });

  describe('in a browser with native drag&drop scroll support', () => {
    beforeEach(() => {
      ScrollService._hasNativeSupport = true;
    });

    it('should not start/stop scrolling on mouse enter/leave events', () => {
      const startScroll = spyOn(ScrollService, 'startScrolling');
      const stopScroll = spyOn(ScrollService, 'stopScrolling');
      ScrollService.enable();

      baseJQueryElement.trigger('mouseleave');
      expect(startScroll).not.toHaveBeenCalled();

      baseJQueryElement.trigger('mouseenter');
      expect(stopScroll).not.toHaveBeenCalled();
    });

    it('should not stop scrolling or remove event listeners when disabled', () => {
      const baseEventSpy = spyOn(baseJQueryElement, 'off').and.callThrough();
      const stopScroll = spyOn(ScrollService, 'stopScrolling');
      ScrollService.disable();

      expect(stopScroll).not.toHaveBeenCalled();
      expect(baseEventSpy).not.toHaveBeenCalled();
    });
  });

  describe('in a browser without native drag&drop scroll support', () => {
    beforeEach(() => {
      ScrollService._hasNativeSupport = false;
    });

    it('should start/stop scrolling on mouse enter/leave events', () => {
      const startScroll = spyOn(ScrollService, 'startScrolling');
      const stopScroll = spyOn(ScrollService, 'stopScrolling');
      ScrollService.enable();

      baseJQueryElement.trigger('mouseleave');
      expect(startScroll).toHaveBeenCalled();

      baseJQueryElement.trigger('mouseenter');
      expect(stopScroll).toHaveBeenCalled();
    });

    it('should not start scrolling when the shouldScroll argument returns false', () => {
      const startScroll = spyOn(ScrollService, 'startScrolling');
      ScrollService.enable(() => false);

      baseJQueryElement.trigger('mouseleave');
      expect(startScroll).not.toHaveBeenCalled();
    });

    it('should stop velocity autoscroll queue and remove event listeners when disabled', () => {
      const baseEventSpy = spyOn(baseJQueryElement, 'off').and.callThrough();
      ScrollService.disable();

      expect(velocitySpy).toHaveBeenCalledWith('stop', 'autoscroll');
      expect(baseEventSpy).toHaveBeenCalled();
    });

    it('should not scroll up when at the top and mouse leaves on top', () => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      ScrollService.startScrolling(null, 10);

      expect(ScrollService._scroll).not.toHaveBeenCalled();
    });

    it('should scroll down when at the top and mouse leaves at the bottom', () => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      ScrollService.startScrolling(null, 260);

      expect(ScrollService._scroll).toHaveBeenCalledWith(200);
    });

    it('should not scroll down when at the bottom and mouse leaves at the bottom', () => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      baseJQueryElement.scrollTop(200);
      ScrollService.startScrolling(null, 260);

      expect(ScrollService._scroll).not.toHaveBeenCalled();
    });

    it('should not scroll down when at the bottom and mouse leaves at the bottom', () => {
      spyOn(ScrollService, '_scroll');

      ScrollService.enable();
      baseJQueryElement.scrollTop(200);
      ScrollService.startScrolling(null, 10);

      expect(ScrollService._scroll).toHaveBeenCalledWith(-200);
    });
  });
});
