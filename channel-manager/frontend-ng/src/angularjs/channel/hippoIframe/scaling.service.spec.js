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

describe('ScalingService', function () {
  'use strict';

  var ScalingService;
  var iframeJQueryElement;
  var baseJQueryElement;
  var canvasJQueryElement;
  var elementsToScale;

  beforeEach(function () {
    module('hippo-cm.channel.hippoIframe');

    inject(function (_ScalingService_) {
      ScalingService = _ScalingService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/scaling.service.fixture.html');

    iframeJQueryElement = $j('#test-hippo-iframe');
    baseJQueryElement = $j('.channel-iframe-base');
    canvasJQueryElement = $j('.channel-iframe-canvas');
    elementsToScale = jasmine.createSpyObj('elementsToScale', ['velocity']);

    spyOn(iframeJQueryElement, 'find').and.callFake(function (selector) {
      return selector === '.cm-scale' ? elementsToScale : $j('#test-hippo-iframe ' + selector);
    });
  });

  afterEach(function () {
    // prevent subsequent window.resize events by Karma from triggering the scaling service
    ScalingService.init(null);
  });

  it('should initialize the scaling factor instantly to 1.0', function () {
    ScalingService.init(iframeJQueryElement);

    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 1.0,
      },
      {
        duration: 0,
        easing: ScalingService.scaleEasing,
      }
    );
    expect(ScalingService.scaleFactor).toEqual(1.0);
  });

  it('should change the scaling factor animated when setting pushWidth', function () {
    canvasJQueryElement.width(400);

    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);

    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 0.75,
      },
      {
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      }
    );
    expect(ScalingService.scaleFactor).toEqual(0.75);
  });

  it('should reset the scaling factor animated to 1.0 when setting pushWidth to 0', function () {
    canvasJQueryElement.width(400);

    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);
    elementsToScale.velocity.calls.reset();

    ScalingService.setPushWidth(0);

    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 1.0,
      },
      {
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      }
    );
    expect(ScalingService.scaleFactor).toEqual(1.0);
  });

  it('should change the scaling factor instantly when the window is resized', function () {
    canvasJQueryElement.width(200);
    ScalingService.init(iframeJQueryElement);
    ScalingService.setPushWidth(100);
    elementsToScale.velocity.calls.reset();

    $j(window).resize();

    expect(elementsToScale.velocity).toHaveBeenCalledWith('finish');
    expect(elementsToScale.velocity).toHaveBeenCalledWith(
      {
        scale: 0.5,
      },
      {
        duration: 0,
        easing: ScalingService.scaleEasing,
      }
    );
    expect(ScalingService.scaleFactor).toEqual(0.5);
  });

  it('should update the scroll position instantly while scaling', function () {
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
      {
        duration: ScalingService.scaleDuration,
        easing: ScalingService.scaleEasing,
      }
    );
    expect(ScalingService.scaleFactor).toEqual(0.75);
  });

});
