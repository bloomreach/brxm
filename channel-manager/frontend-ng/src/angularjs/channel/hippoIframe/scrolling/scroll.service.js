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

const EVENT_NAMESPACE = '.scroll-events';
const DURATION_MIN = 500;
const DURATION_MAX = 1500;

class ScrollService {

  constructor(BrowserService, DomService) {
    'ngInject';

    this.BrowserService = BrowserService;
    this.DomService = DomService;
  }

  init(iframe, canvas, sheet) {
    this.iframe = iframe;
    this.sheet = sheet;
    this.canvas = canvas;
    this.enabled = false;

    this.savedScrollPosition = {
      top: 0,
      canvasLeft: 0,
      iframeLeft: 0,
    };
  }

  enable() {
    if (!this.enabled && this.iframe) {
      this._initIframeElements();
      if (this.BrowserService.isFF()) {
        this._bindMouseMove();
      } else {
        this._bindMouseEnterMouseLeave();
      }
      this.enabled = true;
    }
  }

  disable() {
    if (this.enabled && this.iframe) {
      this._stopScrolling();
      if (this.BrowserService.isFF()) {
        this._unbindMouseMove();
      } else {
        this._unbindMouseEnterMouseLeave();
      }
      this.enabled = false;
    }
  }

  _initIframeElements() {
    this.iframeWindow = $(this.iframe[0].contentWindow);
    this.iframeDocument = this.iframe.contents();
    this.iframeHtmlBody = this.iframeDocument.find('html, body');
    this.iframeBody = this.iframeDocument.find('body');
  }

  _bindMouseEnterMouseLeave() {
    this.iframe
      .on(`mouseenter${EVENT_NAMESPACE}`, () => this._stopScrolling())
      .on(`mouseleave${EVENT_NAMESPACE}`, event => this._startScrolling(event.pageX, event.pageY),
    );
  }

  _unbindMouseEnterMouseLeave() {
    this.iframe.off(EVENT_NAMESPACE);
  }

  _bindMouseMove() {
    const iframe = {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
    };
    let iframeX;
    let iframeY;
    let mouseHasLeft = false;
    let target = null;

    const loadCoords = () => {
      const { top, right, bottom, left, targetX } = this._getScrollData();
      iframeY = top;
      iframeX = left;
      iframe.bottom = bottom - top;
      iframe.right = right - left;
      target = targetX;
    };

    loadCoords();
    this.iframeWindow.on(`resize${EVENT_NAMESPACE}`, loadCoords);

    this.iframeDocument.on(`mousemove${EVENT_NAMESPACE}`, (event) => {
      // event pageX&Y coordinates are relative to the iframe, but expected to be relative to the NG app.
      const scrollLeft = target === this.canvas ? this.canvas.scrollLeft() : this.iframeWindow.scrollLeft();
      const scrollTop = this.iframeWindow.scrollTop();
      const pageX = event.pageX - scrollLeft;
      const pageY = event.pageY - scrollTop;

      if (mouseHasLeft) {
        if (pageX > iframe.left && pageX < iframe.right && pageY > iframe.top && pageY < iframe.bottom) {
          // mouse enters
          this._stopScrolling();
          mouseHasLeft = false;
        }
      } else if (pageX <= iframe.left || pageX >= iframe.right || pageY <= iframe.top || pageY >= iframe.bottom) {
        // mouse leaves
        mouseHasLeft = true;
        this._startScrolling(pageX + iframeX, pageY + iframeY);
      }
    });
  }

  _unbindMouseMove() {
    this.iframeWindow.off(EVENT_NAMESPACE);
    this.iframeDocument.off(EVENT_NAMESPACE);
  }

  _startScrolling(mouseX, mouseY) {
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
    } = this._getScrollData();

    const scroll = (to, distance, target) => {
      if (distance > 0) {
        const duration = this._calculateDuration(distance, DURATION_MIN, DURATION_MAX);
        this._scroll(target, to, duration);
      }
    };

    const scrollX = (posX, distance) => {
      scroll({ scrollLeft: posX }, distance, targetX);
    };

    const scrollY = (posY, distance) => {
      scroll({ scrollTop: posY }, distance, targetY);
    };

    if (mouseY <= top) {
      // scroll up
      scrollY(0, scrollTop);
    } else if (mouseY >= bottom) {
      // scroll down
      scrollY(scrollMaxY, scrollMaxY - scrollTop);
    } else if (mouseX <= left) {
      // scroll left
      scrollX(0, scrollLeft);
    } else if (mouseX >= right) {
      // scroll right
      scrollX(scrollMaxX, scrollMaxX - scrollLeft);
    }
  }

  _stopScrolling() {
    if (this.iframeHtmlBody) {
      this.iframeHtmlBody.stop();
    }
    if (this.canvas) {
      this.canvas.stop();
    }
  }

  _scroll(target, to, duration) {
    target.stop().animate(to, {
      duration,
    });
  }

  _calculateDuration(distance, min, max) {
    if (distance === 0) {
      return 0;
    }

    const duration = distance * 2;
    return Math.min(Math.max(min, duration), max);
  }

  _getScrollData() {
    const canvasOffset = this.canvas.offset();
    const canvasWidth = this.canvas.outerWidth();
    const canvasLeft = canvasOffset.left;
    const canvasRight = canvasOffset.left + canvasWidth;

    const sheetOffset = this.sheet.offset();
    const sheetWidth = this.sheet.outerWidth();
    const sheetLeft = sheetOffset.left;
    const sheetRight = sheetOffset.left + sheetWidth;

    const iframeOffset = this.iframe.offset();
    const iframeWidth = this.iframe.outerWidth();
    const iframeHeight = this.iframe.outerHeight();

    const body = this.iframeBody[0];
    const bodyScrollWidth = body.scrollWidth;
    const bodyScrollHeight = body.scrollHeight;

    const top = iframeOffset.top;
    const right = Math.min(canvasRight, sheetRight);
    const bottom = iframeOffset.top + iframeHeight;
    const left = sheetLeft >= 0 ? sheetLeft : canvasLeft;

    const canvasScrollMaxX = iframeWidth - canvasWidth;
    const iframeScrollMaxX = bodyScrollWidth - iframeWidth;
    const useCanvas = iframeWidth > canvasWidth;

    const scrollTop = this.iframeWindow.scrollTop();
    const scrollLeft = useCanvas ? canvasOffset.left - sheetOffset.left : this.iframeWindow.scrollLeft();

    const targetX = useCanvas ? this.canvas : this.iframeHtmlBody;
    const targetY = this.iframeHtmlBody;

    const win = this.iframeWindow[0];
    // Firefox exposes scrollMaxX and scrollMaxY which is exactly what we need. If the property is not a number,
    // we need to calculate it. See https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollMaxX
    let scrollMaxX;
    if (typeof win.scrollMaxX === 'number') {
      scrollMaxX = win.scrollMaxX;
    } else {
      scrollMaxX = useCanvas ? canvasScrollMaxX : iframeScrollMaxX;
    }

    let scrollMaxY;
    if (typeof win.scrollMaxY === 'number') {
      scrollMaxY = win.scrollMaxY;
    } else {
      scrollMaxY = bodyScrollHeight - iframeHeight;
      if (bodyScrollWidth > iframeWidth) {
        // horizontal scrollbar drawn in the site
        scrollMaxY += this.DomService.getScrollBarWidth();
      }
    }

    return {
      // boundary coordinates
      top,
      right,
      bottom,
      left,

      // scroll positions
      scrollLeft,
      scrollTop,

      // max scroll positions
      scrollMaxX,
      scrollMaxY,

      // target elements
      targetX,
      targetY,
    };
  }

  saveScrollPosition() {
    this._initIframeElements();
    this.savedScrollPosition.top = this.iframeWindow.scrollTop();
    this.savedScrollPosition.iframeLeft = this.iframeWindow.scrollLeft();
    this.savedScrollPosition.canvasLeft = this.canvas.scrollLeft();
  }

  restoreScrollPosition() {
    this._initIframeElements();
    this.iframeWindow.scrollTop(this.savedScrollPosition.top);
    this.iframeWindow.scrollLeft(this.savedScrollPosition.iframeLeft);
    this.canvas.scrollLeft(this.savedScrollPosition.canvasLeft);
  }
}

export default ScrollService;
