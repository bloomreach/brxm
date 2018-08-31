/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
  constructor($element, $window) {
    'ngInject';

    this.handle = $element;
    this.$window = $window;
  }

  $onInit() {
    this.add = this.handlePosition === 'left';
    this.handle.addClass(this.handlePosition === 'left' ? 'left' : 'right');
    this._registerEvents(this.element);
  }

  _registerEvents(manipulatedElement) {
    this.handle.on('mousedown', (mouseDownEvent) => {
      this.maxWidth = Math.floor($('body').width() / 2);

      const initialX = mouseDownEvent.pageX;
      const initialWidth = manipulatedElement.width();

      const mask = this._createMask();
      mask.on('mousemove', (moveEvent) => {
        const diff = initialX - moveEvent.pageX;
        let newWidth = this.add ? initialWidth + diff : initialWidth - diff;

        if (newWidth < this.minWidth) newWidth = this.minWidth;
        if (newWidth > this.maxWidth) newWidth = this.maxWidth;

        if (newWidth !== manipulatedElement.width()) {
          manipulatedElement.css('width', newWidth);
          this.onResize({ newWidth });
        }
      });

      mask.on('mouseup', () => {
        mask.hide();
        mask.off('mousemove');
        mask.off('mouseup');
        mask.remove();
        // to trigger hiding/showing pagination handles of md-tabs
        this.$window.dispatchEvent(new Event('resize'));
      });

      mask.show();
    });
  }

  _createMask() {
    return $('<div class="resize-handle-mask"></div>').appendTo('body');
  }
}

export default resizeHandleController;
