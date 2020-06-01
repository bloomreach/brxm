/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

export default class ScrollService {
  constructor($document, $q, $rootScope, $window, CommunicationService) {
    'ngInject';

    this.$document = $document;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$window = $window;
    this.CommunicationService = CommunicationService;

    this._onMouseMove = this._onMouseMove.bind(this);
    this._onScrollUpdate = this._onScrollUpdate.bind(this);
  }

  getScroll() {
    const body = this.$document.find('body')[0];

    return {
      scrollLeft: this.$window.scrollX,
      scrollTop: this.$window.scrollY,
      scrollWidth: body.scrollWidth,
      scrollHeight: body.scrollHeight,

      // Firefox exposes scrollMaxX and scrollMaxY which is exactly what we need. If the property is not a number,
      // we need to calculate it. See https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollMaxX
      scrollMaxX: typeof this.$window.scrollMaxX === 'number' ? this.$window.scrollMaxX : undefined,
      scrollMaxY: typeof this.$window.scrollMaxY === 'number' ? this.$window.scrollMaxY : undefined,
    };
  }

  // eslint-disable-next-line consistent-return
  setScroll(scroll, duration) {
    const element = this.$document.find('html, body');

    if (duration) {
      return this.$q(done => element.stop().animate(scroll, { duration, done }));
    }

    if (scroll.scrollLeft != null) {
      element.scrollLeft(scroll.scrollLeft);
    }

    if (scroll.scrollTop != null) {
      element.scrollTop(scroll.scrollTop);
    }
  }

  stopScroll() {
    this.$document.find('html, body').stop();
  }

  disable() {
    this.$window.removeEventListener('resize', this._onScrollUpdate);
    this.$document.off('mousemove', this._onMouseMove);
  }

  async enable() {
    await this._onScrollUpdate();
    this._mouseHasLeft = false;

    this.$window.addEventListener('resize', this._onScrollUpdate);
    this.$document.on('mousemove', this._onMouseMove);
  }

  async _onScrollUpdate() {
    this._canvasScroll = await this.CommunicationService.getScroll();
  }

  _onMouseMove(event) {
    // event pageX&Y coordinates are relative to the iframe, but expected to be relative to the NG app.
    const offsetX = this._canvasScroll.targetX === 'canvas' ? this._canvasScroll.scrollLeft : this.$window.scrollX;
    const offsetY = this.$window.scrollY;
    const pageX = event.pageX - offsetX;
    const pageY = event.pageY - offsetY;
    const isInsideIframe = pageX > 0
      && pageX < this._canvasScroll.right - this._canvasScroll.left
      && pageY > 0
      && pageY < this._canvasScroll.bottom - this._canvasScroll.top;

    if (this._mouseHasLeft && isInsideIframe) {
      this.CommunicationService.emit('scroll:stop');
      this._onScrollUpdate();
      this._mouseHasLeft = false;
    } else if (!this._mouseHasLeft && !isInsideIframe) {
      this.CommunicationService.emit('scroll:start', {
        pageX: pageX + this._canvasScroll.left,
        pageY: pageY + this._canvasScroll.top,
      });
      this._mouseHasLeft = true;
    }
  }
}
