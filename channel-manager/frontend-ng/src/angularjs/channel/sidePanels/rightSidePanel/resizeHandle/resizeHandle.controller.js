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

class resizeHandleController {
  constructor($element, $document, $scope) {
    'ngInject';

    this.$document = $document;
    this.$scope = $scope;
    this.handle = $element;
    this.maxWidth = $('body').width() / 2;
    this.hippoOverlay = {};
    this.hippoOverlayTransitionTime = this.hippoOverlayTransitionTime;
  }

  $onInit() {
    this._registerEvents(this.element);
  }

  _registerEvents(manipulatedElement) {
    this.handle.mousedown((mouseDownEvent) => {
      let newWidth;
      const initialWidth = manipulatedElement.width();
      const initialX = mouseDownEvent.pageX;
      const hippoIframe = $('hippo-iframe').find('iframe');
      hippoIframe.css('pointer-events', 'none');
      manipulatedElement.addClass('in-resize');

      this.hippoOverlay = hippoIframe.contents().find('.hippo-overlay');
      this._hideHippoOverlay();

      this.$document.mousemove((moveEvent) => {
        const diff = initialX - moveEvent.pageX;
        newWidth = initialWidth + diff;

        if (newWidth < 440) newWidth = 440;
        if (newWidth > this.maxWidth) newWidth = this.maxWidth;

        if (manipulatedElement.width() >= 440 && manipulatedElement.width() <= this.maxWidth) {
          manipulatedElement.css('width', newWidth);
          // manipulatedElement.css('max-width', newWidth);
          // this.onResize({ newWidth });
        }
      });

      this.$document.mouseup(() => {
        hippoIframe.css('pointer-events', '');
        this._showHippoOverlay();
        manipulatedElement.removeClass('in-resize');
        this.$document.unbind('mousemove');
        this.$document.unbind('mouseup');
      });
    });
  }

  _hideHippoOverlay() {
    this.hippoOverlay.animate({
      opacity: '0',
    }, this.hippoOverlayTransitionTime);
    setTimeout(() => this.hippoOverlay.css('display', 'none'), this.hippoOverlayTransitionTime);
  }

  _showHippoOverlay() {
    this.hippoOverlay.css('display', '');
    this.hippoOverlay.animate({
      opacity: '1',
    }, this.hippoOverlayTransitionTime);
  }
}

export default resizeHandleController;
