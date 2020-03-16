/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

const DURATION_MIN = 500;
const DURATION_MAX = 1500;

export default class ScrollService {
  constructor($rootScope, BrowserService, CommunicationService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.BrowserService = BrowserService;
    this.CommunicationService = CommunicationService;

    this._onScrollStart = this._onScrollStart.bind(this);
    this._onScrollStop = this._onScrollStop.bind(this);
  }

  init(iframe, canvas, sheet) {
    this.iframe = iframe;
    this.sheet = sheet;
    this.canvas = canvas;
    this.enabled = false;
  }

  getScroll() {
    const canvasOffset = this.canvas.offset();
    const canvasWidth = this.canvas.outerWidth();
    const canvasLeft = canvasOffset.left;
    const canvasRight = canvasOffset.left + canvasWidth;

    const sheetOffset = this.sheet.offset();
    const sheetWidth = this.sheet.outerWidth();
    const sheetLeft = sheetOffset.left;
    const sheetRight = sheetOffset.left + sheetWidth;

    const offset = this.iframe.offset();
    const scrollWidth = this.iframe.outerWidth();
    const scrollHeight = this.iframe.outerHeight();

    const { top } = offset;
    const right = Math.min(canvasRight, sheetRight);
    const bottom = top + scrollHeight;
    const left = sheetLeft >= 0 ? sheetLeft : canvasLeft;

    const scrollLeft = this.canvas.scrollLeft();
    const scrollMaxX = scrollWidth - canvasWidth;

    const targetX = scrollWidth > canvasWidth ? 'canvas' : 'iframe';
    const targetY = 'iframe';

    return {
      // boundary coordinates
      top,
      right,
      bottom,
      left,

      scrollWidth,
      scrollHeight,

      // scroll positions
      scrollLeft,
      scrollMaxX,

      // target elements
      targetX,
      targetY,
    };
  }

  enable() {
    if (this.enabled) {
      return;
    }

    this.enabled = true;

    if (this.BrowserService.isFF()) {
      this._offScrollStart = this.$rootScope.$on('iframe:scroll:start', ($event, data) => this._onScrollStart(data));
      this._offScrollStop = this.$rootScope.$on('iframe:scroll:stop', this._onScrollStop);
      this.CommunicationService.enableScroll();

      return;
    }

    this.iframe
      .on('mouseenter', this._onScrollStop)
      .on('mouseleave', this._onScrollStart);
  }

  disable() {
    if (!this.enabled) {
      return;
    }

    this._onScrollStop();
    this.enabled = false;
    if (this.BrowserService.isFF()) {
      this._offScrollStart();
      this._offScrollStop();
      this.CommunicationService.disableScroll();

      return;
    }

    this.iframe
      .off('mouseenter', this._onScrollStop)
      .off('mouseleave', this._onScrollStart);
  }

  async _onScrollStart({ pageX, pageY }) {
    const {
      top,
      right,
      bottom,
      left,
      scrollLeft,
      scrollTop,
      scrollMaxX,
      scrollMaxY,
      targetX,
      targetY,
    } = await this._getScrollData();
    const scrollX = (pos, distance) => this._scroll(targetX, { scrollLeft: pos }, distance);
    const scrollY = (pos, distance) => this._scroll(targetY, { scrollTop: pos }, distance);

    if (pageY <= top) {
      // scroll up
      scrollY(0, scrollTop);
    } else if (pageY >= bottom) {
      // scroll down
      scrollY(scrollMaxY, scrollMaxY - scrollTop);
    } else if (pageX <= left) {
      // scroll left
      scrollX(0, scrollLeft);
    } else if (pageX >= right) {
      // scroll right
      scrollX(scrollMaxX, scrollMaxX - scrollLeft);
    }
  }

  _onScrollStop() {
    if (this.canvas) {
      this.canvas.stop();
    }

    this.CommunicationService.stopScroll();
  }

  _scroll(target, scroll, distance) {
    if (distance <= 0) {
      return;
    }

    const duration = Math.min(Math.max(DURATION_MIN, distance * 2), DURATION_MAX);

    if (target === 'canvas') {
      this.canvas.stop().animate(scroll, { duration });

      return;
    }

    this.CommunicationService.setScroll(scroll, duration);
  }

  async _getScrollData() {
    const canvasScroll = this.getScroll();
    const iframeScroll = await this.CommunicationService.getScroll();
    const isUsingCanvas = canvasScroll.targetX === 'canvas';
    const scrollLeft = isUsingCanvas ? canvasScroll.scrollLeft : iframeScroll.scrollLeft;

    let { scrollMaxX } = iframeScroll;
    if (isUsingCanvas) {
      ({ scrollMaxX } = canvasScroll);
    } else if (scrollMaxX == null) {
      scrollMaxX = iframeScroll.scrollWidth - canvasScroll.scrollWidth;
    }

    let { scrollMaxY } = iframeScroll;
    if (scrollMaxY == null) {
      scrollMaxY = iframeScroll.scrollHeight - canvasScroll.scrollHeight
        + (iframeScroll.scrollWidth > canvasScroll.scrollWidth ? this._getScrollBarSize() : 0);
    }

    return {
      ...canvasScroll,
      ...iframeScroll,
      scrollLeft,
      scrollMaxX,
      scrollMaxY,
    };
  }

  _getScrollBarSize() {
    if (!this._scrollBarSize) {
      const outerWidth = 100;
      const $outer = angular.element('<div>')
        .css({
          visibility: 'hidden',
          width: outerWidth,
          overflow: 'scroll',
        })
        .appendTo('body');
      const widthWithScroll = angular.element('<div>')
        .css('width', '100%')
        .appendTo($outer)
        .outerWidth();

      $outer.remove();
      this._scrollBarSize = outerWidth - widthWithScroll;
    }

    return this._scrollBarSize;
  }

  async savePosition() {
    const canvasScrollLeft = this.canvas.scrollLeft();
    const {
      scrollLeft: iframeScrollLeft,
      scrollTop: iframeScrollTop,
    } = await this.CommunicationService.getScroll();

    this._position = { canvasScrollLeft, iframeScrollLeft, iframeScrollTop };
  }

  async restorePosition() {
    if (!this._position) {
      return;
    }

    const {
      canvasScrollLeft,
      iframeScrollLeft: scrollLeft,
      iframeScrollTop: scrollTop,
    } = this._position;
    delete this._position;

    this.canvas.scrollLeft(canvasScrollLeft);
    await this.CommunicationService.setScroll({ scrollLeft, scrollTop });
  }
}
