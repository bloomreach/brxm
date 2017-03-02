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

  constructor(BrowserService) {
    'ngInject';

    this.BrowserService = BrowserService;
  }

  init(iframe, canvas) {
    this.iframe = iframe;
    this.canvas = canvas;
    this.enabled = false;

    this.savedScrollPosition = {
      top: 0,
      left: 0,
    };
  }

  enable(scrollAllowed) {
    if (!this.enabled && this.iframe) {
      this._initIframeElements();
      if (this.BrowserService.isFF()) {
        this._bindMouseMove(scrollAllowed);
      } else {
        this._bindMouseEnterMouseLeave(scrollAllowed);
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

  _bindMouseEnterMouseLeave(scrollAllowed) {
    this.iframe
      .on(`mouseenter${EVENT_NAMESPACE}`, () => this._stopScrolling())
      .on(`mouseleave${EVENT_NAMESPACE}`, (event) => {
        if (scrollAllowed()) {
          this._startScrolling(event.pageX, event.pageY);
        }
      });
  }

  _unbindMouseEnterMouseLeave() {
    this.iframe.off(EVENT_NAMESPACE);
  }

  _bindMouseMove(scrollAllowed) {
    const upperBoundary = 0;
    let bottomBoundary;
    let iframeTop;
    let mouseHasLeft = false;

    const loadProperties = () => {
      const { top, bottom } = this._getCoords();
      iframeTop = top;
      bottomBoundary = bottom - top;
    };
    loadProperties();

    const mouseEnters = pageY => pageY > upperBoundary && pageY < bottomBoundary;
    const mouseLeaves = pageY => pageY <= upperBoundary || pageY >= bottomBoundary;

    this.iframeWindow.on(`resize${EVENT_NAMESPACE}`, loadProperties);
    this.iframeDocument.on(`mousemove${EVENT_NAMESPACE}`, (event) => {
      if (scrollAllowed()) {
        // event pageX&Y coordinates are relative to the iframe, but expected to be relative to the NG app.
        const pageY = event.pageY - this._getScrollTop();

        if (mouseHasLeft) {
          if (mouseEnters(pageY)) {
            this._stopScrolling();
            mouseHasLeft = false;
          }
        } else if (mouseLeaves(pageY)) {
          mouseHasLeft = true;
          this._startScrolling(event.pageX, pageY + iframeTop);
        }
      }
    });
  }

  _unbindMouseMove() {
    this.iframeWindow.off(EVENT_NAMESPACE);
    this.iframeDocument.off(EVENT_NAMESPACE);
  }

  _startScrolling(mouseX, mouseY) {
    const { width, height, top, right, bottom, left } = this._getCoords();
    let to;
    let distance;

    if (mouseY <= top) {
      // scroll to the top
      to = { scrollTop: 0 };
      distance = this._getScrollTop();
    } else if (mouseY >= bottom) {
      // scroll to the bottom
      to = { scrollTop: height };
      distance = height - this._getScrollTop();
    } else if (mouseX <= left) {
      // scroll to the left
      to = { scrollLeft: 0 };
      distance = this._getScrollLeft();
    } else if (mouseX >= right) {
      // scroll to the right
      to = { scrollLeft: width };
      distance = width - this._getScrollLeft();
    }

    if (distance > 0) {
      const duration = this._calculateDuration(distance, DURATION_MIN, DURATION_MAX);
      this._scroll(to, duration);
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

  _scroll(to, duration) {
    const target = to.scrollLeft !== undefined ? this.canvas : this.iframeHtmlBody;
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

  _getScrollTop() {
    return this.iframeWindow.scrollTop();
  }

  _setScrollTop(scrollTop) {
    this.iframeHtmlBody.scrollTop(scrollTop);
  }

  _getScrollLeft() {
    return this.canvas.scrollLeft();
  }

  _setScrollLeft(scrollLeft) {
    this.canvas.scrollLeft(scrollLeft);
  }

  _getCoords() {
    const canvasOffset = this.canvas.offset();
    const canvasWidth = this.canvas.outerWidth();
    const iframeHeight = this.iframe.outerHeight();
    const iframeOffset = this.iframe.offset();
    const iframeWidth = this.iframe.outerWidth();
    const pageHeight = this.iframeBody[0].scrollHeight;

    return {
      width: iframeWidth - canvasWidth,
      height: pageHeight - iframeHeight,
      top: iframeOffset.top,
      right: canvasOffset.left + canvasWidth,
      bottom: iframeOffset.top + iframeHeight,
      left: canvasOffset.left,
    };
  }

  saveScrollPosition() {
    this._initIframeElements();
    this.savedScrollPosition.top = this._getScrollTop();
    this.savedScrollPosition.left = this._getScrollLeft();
  }

  restoreScrollPosition() {
    this._initIframeElements();
    this._setScrollTop(this.savedScrollPosition.top);
    this._setScrollLeft(this.savedScrollPosition.left);
  }
}

export default ScrollService;
