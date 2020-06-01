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

const MOUSE_LEFT = 1;

class resizeHandleController {
  constructor($element) {
    'ngInject';

    this.handle = $element;
  }

  $onInit() {
    this.isInversed = this.handlePosition === 'left';
    this.handle
      .addClass(this.handlePosition === 'left' ? 'left' : 'right')
      .on('mousedown', this._onMouseDown.bind(this));
  }

  _resize(newWidth) {
    if (newWidth === this.element.width()) {
      return;
    }

    this.element.css('width', newWidth);
    this.onResize({ newWidth });
  }

  _onMouseDown(event) {
    if (event.which !== MOUSE_LEFT) {
      return;
    }

    event.preventDefault();

    this.offset = event.pageX;
    this.initialWidth = this.element.width();
    this.maxWidth = Math.floor($('body').width() / 2);

    this._createMask();
  }

  _onMouseMove(event) {
    if (!event.buttons) {
      this._removeMask();

      return;
    }

    const diff = event.pageX - this.offset;
    let newWidth = this.isInversed ? this.initialWidth - diff : this.initialWidth + diff;
    newWidth = Math.max(newWidth, this.minWidth);
    newWidth = Math.min(newWidth, this.maxWidth);

    this._resize(newWidth);
  }

  _createMask() {
    this.mask = $('<div class="resize-handle-mask"></div>')
      .on('mousemove', this._onMouseMove.bind(this))
      .on('mouseup', this._removeMask.bind(this))
      .show()
      .appendTo('body');
  }

  _removeMask() {
    this.mask.hide()
      .off('mousemove')
      .off('mouseup')
      .remove();
  }
}

export default resizeHandleController;
