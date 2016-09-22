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

export class ScalingService {

  constructor($rootScope, $window, OverlaySyncService, DomService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.OverlaySyncService = OverlaySyncService;
    this.DomService = DomService;

    this.panels = {
      left: {
        pushWidth: 0,
      },
      right: {
        pushWidth: 0,
      },
    };
    this.viewPortWidth = 0; // unconstrained
    this.scaleFactor = 1.0;
    this.scaleDuration = 400; // matches angular material
    this.scaleEasing = [0.25, 0.8, 0.25, 1]; // matches angular material

    angular.element($window).bind('resize', () => {
      if (this.hippoIframeJQueryElement) {
        $rootScope.$apply(() => this._resyncScaling(false));
      }
    });
  }

  init(hippoIframeJQueryElement) {
    this.hippoIframeJQueryElement = hippoIframeJQueryElement;
    this._resyncScaling(false);
  }

  setPushWidth(side, pushWidth) {
    this.panels[side].pushWidth = pushWidth;
    this._updateScaling(side, true);
  }

  setViewPortWidth(viewPortWidth) {
    this.OverlaySyncService.setViewPortWidth(viewPortWidth);
    this._resyncScaling(false);
    this.OverlaySyncService.syncIframe();
  }

  getScaleFactor() {
    return this.scaleFactor;
  }

  /**
   * Update the iframe shift, if necessary
   *
   * The iframe should be shifted right (by controlling the translateX) if the left side panel is open,
   * and if the viewport width is less than the available canvas
   *
   * @returns {*[]}  canvasWidth is the maximum width available to the iframe
   *                 viewPortWidth indicates how many pixels wide the iframe content should be rendered.
   */
  _updateIframeShift(side) {
    const negativeOrPositiveAdjust = (side === 'left' ? '' : '-');
    const transform = this.hippoIframeJQueryElement.css('transform');
    const transformXValue = transform.split(',')[5] || 0;
    const currentShift = parseInt((transformXValue), 10);
    const canvasWidth = this.hippoIframeJQueryElement.find('.channel-iframe-canvas').width() + currentShift;
    const viewPortWidth = this.OverlaySyncService.getViewPortWidth() === 0 ? canvasWidth : this.OverlaySyncService.getViewPortWidth();
    const canvasBorderWidth = canvasWidth - viewPortWidth;
    const targetShift = Math.min(canvasBorderWidth, this.panels[side].pushWidth);

    this.hippoIframeJQueryElement.css('transform', `translateX(${negativeOrPositiveAdjust}${targetShift})`);

    return [canvasWidth, viewPortWidth];
  }

  /**
   * Update the scale factor, if necessary
   *
   * We compute the new scale factor and compare it to the old one. In case of a change, we zoom the "elementsToScale",
   * i.e. the iframe and the overlay, in or out. In case the scale factor changes due to opening/closing the left side panel,
   * which is animated by material, we also animate the zooming and do an attempt to keep the scroll position of the iframe unchanged.
   */
  _updateScaling(side) {
    if (!this.hippoIframeJQueryElement || !this.hippoIframeJQueryElement.is(':visible')) {
      return;
    }

    const [canvasWidth, viewPortWidth] = this._updateIframeShift(side);
    const visibleCanvasWidth = canvasWidth - this.panels[side].pushWidth;
    const oldScale = this.scaleFactor;
    const newScale = (visibleCanvasWidth < viewPortWidth) ? visibleCanvasWidth / viewPortWidth : 1;

    if (newScale !== oldScale) {
      const elementsToScale = this.hippoIframeJQueryElement.find('.cm-scale');
      if (side === 'right') {
        elementsToScale.addClass('cm-scale-to-left');
        elementsToScale.removeClass('cm-scale-to-right');
      } else {
        elementsToScale.removeClass('cm-scale-to-left');
        elementsToScale.addClass('cm-scale-to-right');
      }
      elementsToScale.velocity('finish');

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
      elementsToScale.css('transform', `scale(${newScale})`);
    }
    this.scaleFactor = newScale;
  }

  _resyncScaling() {
    if (!this.hippoIframeJQueryElement || !this.hippoIframeJQueryElement.is(':visible')) {
      return;
    }
    const elementsToScale = this.hippoIframeJQueryElement.find('.cm-scale');
    const side = (elementsToScale.hasClass('cm-scale-to-left') ? 'right' : 'left');

    this._updateScaling(side);
  }
}
