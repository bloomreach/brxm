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

class ScalingService {

  constructor($rootScope, $window, OverlaySyncService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.OverlaySyncService = OverlaySyncService;

    this.pushWidth = 0; // all sidenavs are initially closed
    this.viewPortWidth = 0; // unconstrained
    this.scaleFactor = 1.0;
    this.scaleDuration = ANGULAR_MATERIAL_SIDENAV_ANIMATION_DURATION_MS;
    this.scaleEasing = ANGULAR_MATERIAL_SIDENAV_EASING;

    angular.element($window).bind('resize', () => {
      if (this.hippoIframeJQueryElement) {
        $rootScope.$apply(() => this._updateScaling(false));
      }
    });
  }

  init(hippoIframeJQueryElement) {
    this.hippoIframeJQueryElement = hippoIframeJQueryElement;
    this._updateScaling(false);
  }

  setPushWidth(pushWidth) {
    this.pushWidth = pushWidth;
    this._updateScaling(true);
  }

  setViewPortWidth(viewPortWidth) {
    this.OverlaySyncService.setViewPortWidth(viewPortWidth);
    this.sync();
  }

  sync() {
    this._updateScaling(false);
    this.OverlaySyncService.syncIframe();
  }

  getScaleFactor() {
    return this.scaleFactor;
  }

  /**
   * Update the iframe shift, if necessary
   *
   * The iframe should be shifted right (by controlling the left-margin) if the sidenav is open,
   * and if the viewport width is less than the available canvas
   *
   * @param animate  flag indicating whether any shift-change should be automated or immediate.
   * @returns {*[]}  canvasWidth is the maximum width available to the iframe
   *                 viewPortWidth indicates how many pixels wide the iframe content should be rendered.
   */
  _updateIframeShift(animate) {
    const currentShift = parseInt(this.hippoIframeJQueryElement.css('margin-left'), 10);
    const canvasWidth = this.hippoIframeJQueryElement.find('.channel-iframe-canvas').width() + currentShift;
    const viewPortWidth = this.OverlaySyncService.getViewPortWidth() === 0 ? canvasWidth : this.OverlaySyncService.getViewPortWidth();
    const canvasBorderWidth = canvasWidth - viewPortWidth;
    const targetShift = Math.min(canvasBorderWidth, this.pushWidth);

    if (targetShift !== currentShift) {
      this.hippoIframeJQueryElement.velocity('finish');
      if (animate) {
        this.hippoIframeJQueryElement.velocity({
          'margin-left': targetShift,
        }, {
          duration: this.scaleDuration,
          easing: this.scaleEasing,
        });
      } else {
        this.hippoIframeJQueryElement.css('margin-left', targetShift);
      }
    }

    return [canvasWidth, viewPortWidth];
  }

  /**
   * Update the scale factor, if necessary
   *
   * We compute the new scale factor and compare it to the old one. In case of a change, we zoom the "elementsToScale",
   * i.e. the iframe and the overlay, in or out. In case the scale factor changes due to opening/closing the sidenav,
   * which is animated by material, we also animate the zooming and do an attempt to keep the scroll position of the
   * iframe unchanged. Other changes (window resize, viewport width change) are not animated and we don't worry much
   * about the scroll position.
   *
   * @param animate  flag indicating that any change should be animated.
   */
  _updateScaling(animate) {
    if (!this.hippoIframeJQueryElement || !this.hippoIframeJQueryElement.is(':visible')) {
      return;
    }

    const [canvasWidth, viewPortWidth] = this._updateIframeShift(animate);
    const visibleCanvasWidth = canvasWidth - this.pushWidth;
    const oldScale = this.scaleFactor;
    const newScale = (visibleCanvasWidth < viewPortWidth) ? visibleCanvasWidth / viewPortWidth : 1;

    if (newScale !== oldScale) {
      const elementsToScale = this.hippoIframeJQueryElement.find('.cm-scale');
      elementsToScale.velocity('finish');

      if (animate) {
        const iframeBaseJQueryElement = this.hippoIframeJQueryElement.find('.channel-iframe-base');
        const currentOffset = iframeBaseJQueryElement.scrollTop();
        const targetOffset = oldScale === 1.0 ? newScale * currentOffset : currentOffset / oldScale;

        if (targetOffset !== currentOffset) {
          // keep scroll-position constant during animation
          elementsToScale.velocity('scroll', {
            container: iframeBaseJQueryElement,
            offset: targetOffset - currentOffset,
            duration: this.scaleDuration,
            easing: this.scaleEasing,
            queue: false,
          });
        }

        const iframeScrollXJQueryElement = this.hippoIframeJQueryElement.find('.channel-iframe-scroll-x');
        const widthBeforeScaling = iframeScrollXJQueryElement.width();

        // zoom in/out to new scale factor
        elementsToScale.velocity({
          scale: newScale,
        }, {
          duration: this.scaleDuration,
          easing: this.scaleEasing,
          complete: () => {
            // when scaling causes a scrollbar to appear/disappear, we have to tweak it
            if (newScale !== 1 && widthBeforeScaling !== iframeScrollXJQueryElement.width()) {
              this._updateScaling(animate);
            } else {
              this.OverlaySyncService.syncIframe();
            }
          },
        });
      } else {
        elementsToScale.css('transform', `scale(${newScale})`);
      }
    }

    this.scaleFactor = newScale;
  }

}

export default ScalingService;
