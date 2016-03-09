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

const ANGULAR_MATERIAL_SIDENAV_EASING = [0.25, 0.8, 0.25, 1];
const ANGULAR_MATERIAL_SIDENAV_ANIMATION_DURATION_MS = 400;

export class ScalingService {

  constructor($rootScope, $window) {
    'ngInject';

    this.$rootScope = $rootScope;

    this.pushWidth = 0; // all sidenavs are closed to start with
    this.scaleFactor = 1.0;
    this.scaleDuration = ANGULAR_MATERIAL_SIDENAV_ANIMATION_DURATION_MS;
    this.scaleEasing = ANGULAR_MATERIAL_SIDENAV_EASING;

    angular.element($window).bind('resize', () => {
      if (this.hippoIframeJQueryElement) {
        $rootScope.$apply(() => {
          this._updateScaling();
        });
      }
    });
  }

  init(hippoIframeJQueryElement) {
    this.hippoIframeJQueryElement = hippoIframeJQueryElement;
    this._updateScaling();
  }

  setPushWidth(pushWidth) {
    this.pushWidth = pushWidth;
    if (this.hippoIframeJQueryElement) {
      this._updateScaling();
    }
  }

  _updateScaling() {
    const iframeBaseJQueryElement = this.hippoIframeJQueryElement.find('.channel-iframe-base');
    const elementsToScale = this.hippoIframeJQueryElement.find('.cm-scale');
    const canvasWidth = this.hippoIframeJQueryElement.find('.channel-iframe-canvas').width();
    const visibleCanvasWidth = canvasWidth - this.pushWidth;

    const oldScale = this.scaleFactor;
    const newScale = visibleCanvasWidth / canvasWidth;

    const startScaling = oldScale === 1.0 && newScale !== 1.0;
    const stopScaling = oldScale !== 1.0 && newScale === 1.0;
    const animationDuration = (startScaling || stopScaling) ? this.scaleDuration : 0;

    elementsToScale.velocity('finish');

    if (startScaling || stopScaling) {
      const currentOffset = iframeBaseJQueryElement.scrollTop();
      const targetOffset = startScaling ? newScale * currentOffset : currentOffset / oldScale;

      elementsToScale.velocity('scroll', {
        container: iframeBaseJQueryElement,
        offset: targetOffset - currentOffset,
        duration: animationDuration,
        easing: this.scaleEasing,
        queue: false,
      });
    }

    elementsToScale.velocity({
      scale: newScale,
    }, {
      duration: animationDuration,
      easing: this.scaleEasing,
    });

    this.scaleFactor = newScale;
  }

}
