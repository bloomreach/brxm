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

export class HippoIframeCtrl {
  constructor($scope, $rootScope, $element, $window, linkProcessorService, hstCommentsProcessorService, ChannelService,
              PageStructureService, OverlaySyncService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.linkProcessorService = linkProcessorService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.ChannelService = ChannelService;
    this.PageStructureService = PageStructureService;
    this.OverlaySyncService = OverlaySyncService;

    this.iframeJQueryElement = $element.find('iframe');
    this.iframeJQueryElement.on('load', () => this.onLoad());

    OverlaySyncService.init(this.iframeJQueryElement, $element.find('.overlay'));

    this.elementsToScale = $('.cm-scale');
    this.scrollTop = 0;
    this.scaleFactor = 1.0;
    this.scaleDuration = ANGULAR_MATERIAL_SIDENAV_ANIMATION_DURATION_MS;
    this.scaleEasing = ANGULAR_MATERIAL_SIDENAV_EASING;

    $element.scroll(() => {
      this.scrollTop = $element.scrollTop();
    });

    $scope.$watch('iframe.pushWidth', () => {
      this._updateScaling($element);
    });

    angular.element($window).bind('resize', () => {
      $rootScope.$apply(() => {
        this._updateScaling($element);
      });
    });
  }

  onLoad() {
    this.$rootScope.$apply(() => {

      this._parseHstComments();
      this._parseLinks();

      this.OverlaySyncService.startObserving();
    });
  }

  showComponentProperties(structureElement) {
    this.PageStructureService.showComponentProperties(structureElement);
  }

  _parseHstComments() {
    const iframeDom = this.iframeJQueryElement.contents()[0];

    this.PageStructureService.clearParsedElements();
    this.hstCommentsProcessorService.run(iframeDom,
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService));
    this.PageStructureService.printParsedElements();
  }

  _parseLinks() {
    const iframeDom = this.iframeJQueryElement.contents()[0];
    const internalLinkPrefix = `${iframeDom.location.protocol}//${iframeDom.location.host}${this.ChannelService.getUrl()}`;

    this.linkProcessorService.run(iframeDom, internalLinkPrefix);
  }

  getContainers() {
    return this.selectMode ? this.PageStructureService.containers : [];
  }

  _updateScaling($element) {
    const canvasWidth = $element.find('.channel-iframe-canvas').width();
    const visibleCanvasWidth = canvasWidth - this.pushWidth;

    const oldScale = this.scaleFactor;
    const newScale = visibleCanvasWidth / canvasWidth;

    const startScaling = oldScale === 1.0 && newScale !== 1.0;
    const stopScaling = oldScale !== 1.0 && newScale === 1.0;
    const animationDuration = (startScaling || stopScaling) ? this.scaleDuration : 0;

    this.elementsToScale.velocity('finish');

    if (startScaling || stopScaling) {
      this.elementsToScale.velocity('scroll', {
        container: $element,
        offset: startScaling ? newScale * this.scrollTop : this.scrollTop / oldScale,
        duration: animationDuration,
        easing: this.scaleEasing,
        queue: false,
      });
    }

    this.elementsToScale.velocity({
      scale: newScale,
    }, {
      duration: animationDuration,
      easing: this.scaleEasing,
    });

    this.scaleFactor = newScale;
  }

}
