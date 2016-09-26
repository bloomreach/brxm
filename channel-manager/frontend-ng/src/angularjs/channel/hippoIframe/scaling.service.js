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
        $rootScope.$apply(() => this._updateScaling());
      }
    });
  }

  init(hippoIframeJQueryElement) {
    this.hippoIframeJQueryElement = hippoIframeJQueryElement;
    this._updateScaling();
  }

  setPushWidth(side, pushWidth) {
    this.panels[side].pushWidth = pushWidth;
    this._updateScaling(side);
  }

  setViewPortWidth(viewPortWidth) {
    this.OverlaySyncService.setViewPortWidth(viewPortWidth);
    this._updateScaling();
    this.OverlaySyncService.syncIframe();
  }

  getScaleFactor() {
    return this.scaleFactor;
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

    const iframeWrapper = this.hippoIframeJQueryElement.parent();
    const iframeWrapperWidth = iframeWrapper.width();
    const iframeContentWidth = iframeWrapperWidth - (this.panels.left.pushWidth + this.panels.right.pushWidth);
    const oldScale = this.scaleFactor;

    if (iframeWrapperWidth > 1280) {
      return; // only scale when we're below 1280 width as that is our lowest supported width
    }

    const newScale = Math.min(iframeContentWidth / iframeWrapperWidth, 1);

    if (newScale !== oldScale) {
      const iframeBase = this.hippoIframeJQueryElement.find('.channel-iframe-base');
      const elementsToScale = this.hippoIframeJQueryElement.find('.cm-scale');
      elementsToScale.velocity('finish');
      const currentOffset = iframeBase.scrollTop();
      const targetOffset = oldScale === 1.0 ? newScale * currentOffset : currentOffset / oldScale;

      if (targetOffset !== currentOffset) {
        elementsToScale.velocity('scroll', {
          container: iframeBase,
          offset: targetOffset - currentOffset,
          duration: this.scaleDuration,
          easing: this.scaleEasing,
          queue: false,
        }); // keep scroll-position constant during animation
      }

      elementsToScale.css('transform', `scale(${newScale})`);

      const iframeCanvas = this.hippoIframeJQueryElement.find('.channel-iframe-canvas');

      const leftMargin = Math.abs(parseInt(iframeCanvas.css('margin-left'), 10));
      const rightMargin = Math.abs(parseInt(iframeCanvas.css('margin-right'), 10));

      if (leftMargin !== this.panels.left.pushWidth) {
        iframeCanvas.css('margin-left', `-${this.panels.left.pushWidth}px`);
      }
      if (rightMargin !== this.panels.right.pushWidth) {
        iframeCanvas.css('margin-right', `-${this.panels.right.pushWidth}px`);
      }

      if (this.panels.left.pushWidth && this.panels.right.pushWidth) {
        elementsToScale.css('transform-origin', 'top center');
      } else if ((!this.panels.left.pushWidth && this.panels.right.pushWidth) || side === 'right') {
        elementsToScale.css('transform-origin', 'top left');
      } else if ((this.panels.left.pushWidth && !this.panels.right.pushWidth) || side === 'left') {
        elementsToScale.css('transform-origin', 'top right');
      }
    }
    this.scaleFactor = newScale;
  }
}
