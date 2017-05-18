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
  constructor($element, $document) {
    'ngInject';

    this.$document = $document;
    this.handle = $element;
  }

  $onInit() {
    this._registerEvents(this.element);
  }

  _registerEvents() {
    this.handle.mousedown((mouseDownEvent) => {
      let newWidth;
      const hippoIframe = $('hippo-iframe').find('iframe');
      hippoIframe.css('pointer-events', 'none');
      const initialWidth = this.element.width();
      const initialX = mouseDownEvent.clientX;
      this.element.addClass('in-resize');

      this.$document.mousemove((moveEvent) => {
        const diff = initialX - moveEvent.pageX;
        newWidth = initialWidth + diff;

        if (newWidth < 440 || newWidth > 880) return;

        this.element.css('width', newWidth);
        this.element.css('max-width', newWidth);
      });

      this.$document.mouseup(() => {
        this.$document.unbind('mousemove');
        hippoIframe.css('pointer-events', 'auto');
        this.onResize({ newWidth });
        this.element.removeClass('in-resize');
      });
    });
  }
}

export default resizeHandleController;
