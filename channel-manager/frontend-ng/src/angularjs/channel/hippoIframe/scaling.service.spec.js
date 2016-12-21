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

describe('ScalingService', () => {
  'use strict';

  let ScalingService;
  let iframeJQueryElement;
  let baseJQueryElement;
  let canvasJQueryElement;
  let elementsToScale;

  beforeEach(() => {
    module('hippo-cm.channel.hippoIframe');

    inject((_ScalingService_) => {
      ScalingService = _ScalingService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/scaling.service.fixture.html');

    baseJQueryElement = $j('.channel-iframe-base');
    canvasJQueryElement = $j('.channel-iframe-canvas');
    elementsToScale = jasmine.createSpyObj('elementsToScale', ['velocity', 'css', 'scrollTop']);

    iframeJQueryElement = jasmine.createSpyObj('iframeJQueryElement', ['find', 'css', 'velocity', 'is']);
    iframeJQueryElement.find.and.callFake((selector) => {
      if (selector === '.cm-scale') {
        return elementsToScale;
      }
      return $j(`#test-hippo-iframe ${selector}`);
    });
    iframeJQueryElement.css.and.callFake(() => '0');
    iframeJQueryElement.is.and.returnValue(true);
  });

  afterEach(() => {
    // prevent subsequent window.resize events by Karma from triggering the scaling service
    ScalingService.init(null);
  });

  it('should initialize using the default values', () => {
    ScalingService.init(iframeJQueryElement);

    expect(elementsToScale.velocity).not.toHaveBeenCalled();
    expect(elementsToScale.css).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(1.0);
  });

  it('should change the scaling factor animated when setting pushWidth', () => {
    canvasJQueryElement.width(400);

    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);

    expect(iframeJQueryElement.velocity).not.toHaveBeenCalled();
    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 0.75,
      },
      jasmine.objectContaining({
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      })
    );
    expect(elementsToScale.css).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(0.75);
  });

  it('should reset the scaling factor animated to 1.0 when setting pushWidth to 0', () => {
    canvasJQueryElement.width(400);

    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);
    elementsToScale.velocity.calls.reset();

    ScalingService.setPushWidth(0);

    expect(iframeJQueryElement.velocity).not.toHaveBeenCalled();
    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 1.0,
      },
      jasmine.objectContaining({
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      })
    );
    expect(elementsToScale.css).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(1.0);
  });

  it('should change the scaling factor instantly when the window is resized', () => {
    canvasJQueryElement.width(200);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);
    elementsToScale.velocity.calls.reset();
    ScalingService.scaleFactor = 0.75; // fake different scaling factor so the effect of the window resize is testable

    $j(window).resize();

    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.css).toHaveBeenCalledWith('transform', 'scale(0.5)');
    expect(ScalingService.getScaleFactor()).toEqual(0.5);
  });

  it('should update the scroll position instantly while scaling', () => {
    canvasJQueryElement.width(400);

    // make the canvas scroll within the base element
    baseJQueryElement.height(10);
    canvasJQueryElement.height(500);

    baseJQueryElement.scrollTop(100);

    ScalingService.init(iframeJQueryElement);
    elementsToScale.velocity.calls.reset();

    ScalingService.setPushWidth(100);

    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith('scroll', {
      container: baseJQueryElement,
      offset: -25,
      duration: ScalingService.scaleDuration,
      easing: ScalingService.scaleEasing,
      queue: false,
    });
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 0.75,
      },
      jasmine.objectContaining({
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      })
    );
    expect(ScalingService.getScaleFactor()).toEqual(0.75);
  });

  it('should do nothing when the viewport width of an uninitialized service is changed', () => {
    ScalingService.setViewPortWidth(720);

    expect(iframeJQueryElement.css).not.toHaveBeenCalled();
    expect(elementsToScale.css).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(1.0);
  });

  it('should keep the scale factor unchanged when not constraining the viewport width', () => {
    canvasJQueryElement.width(800);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setViewPortWidth(0);

    expect(iframeJQueryElement.css.calls.mostRecent().args).toEqual(['margin-left']);
    expect(elementsToScale.velocity).not.toHaveBeenCalled();
    expect(iframeJQueryElement.velocity).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(1.0);
  });

  it('should keep the scale factor unchanged when constraining the viewport width smaller than the canvas', () => {
    canvasJQueryElement.width(800);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setViewPortWidth(720);

    expect(iframeJQueryElement.css.calls.mostRecent().args).toEqual(['margin-left']);
    expect(elementsToScale.velocity).not.toHaveBeenCalled();
    expect(iframeJQueryElement.velocity).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(1.0);
  });

  it('should start scaling when constraining the viewport width larger than the canvas', () => {
    canvasJQueryElement.width(400);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setViewPortWidth(800);

    expect(elementsToScale.css.calls.mostRecent().args).toEqual(['transform', 'scale(0.5)']);
    expect(elementsToScale.velocity.calls.mostRecent().args).toEqual(['finish']);
    expect(iframeJQueryElement.velocity.calls.mostRecent().args).toEqual(['finish']);
    expect(iframeJQueryElement.css.calls.mostRecent().args).toEqual(['margin-left', -400]);
    expect(ScalingService.getScaleFactor()).toEqual(0.5);
  });

  it('should start shifting and scaling when pushing the visible canvas width below the viewport width', () => {
    canvasJQueryElement.width(800);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setViewPortWidth(720);

    // reset all relevant spies
    elementsToScale.css.calls.reset();
    elementsToScale.velocity.calls.reset();
    iframeJQueryElement.css.calls.reset();
    iframeJQueryElement.velocity.calls.reset();

    ScalingService.setPushWidth(260);

    // validate shifting
    expect(iframeJQueryElement.velocity).toHaveBeenCalledWith('finish');
    expect(iframeJQueryElement.velocity.calls.mostRecent().args).toEqual([{
      'margin-left': 80,
    }, {
      duration: ScalingService.scaleDuration,
      easing: ScalingService.scaleEasing,
    }]);
    expect(iframeJQueryElement.css.calls.mostRecent().args).toEqual(['margin-left']);

    // validate scaling
    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity.calls.mostRecent().args).toEqual([
      {
        scale: 0.75,
      },
      jasmine.objectContaining({
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      }),
    ]);
    expect(elementsToScale.css).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toBe(0.75);
  });

  it('should stop scaling the pushed iframe when the viewport width drops below the visible canvas width', () => {
    canvasJQueryElement.width(800);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setViewPortWidth(720);
    ScalingService.setPushWidth(260);

    // reset all relevant spies
    elementsToScale.css.calls.reset();
    elementsToScale.velocity.calls.reset();
    iframeJQueryElement.css.calls.reset();
    iframeJQueryElement.velocity.calls.reset();

    iframeJQueryElement.css.and.returnValue(80); // current shift
    canvasJQueryElement.width(720); // current canvas width

    ScalingService.setViewPortWidth(360);

    // validate shifting
    expect(iframeJQueryElement.css.calls.mostRecent().args).toEqual(['margin-left', 260]);

    // validate scaling
    expect(elementsToScale.velocity.calls.mostRecent().args).toEqual(['finish']);
    expect(elementsToScale.css.calls.mostRecent().args).toEqual(['transform', 'scale(1)']);
    expect(ScalingService.getScaleFactor()).toBe(1);
  });

  it('should skip scaling if the iframe element is not visible', () => {
    iframeJQueryElement.is.and.returnValue(false);
    canvasJQueryElement.width(400);

    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);

    expect(iframeJQueryElement.velocity).not.toHaveBeenCalled();
    expect(elementsToScale.velocity).not.toHaveBeenCalled();
    expect(ScalingService.getScaleFactor()).toEqual(1.0);
  });
});
