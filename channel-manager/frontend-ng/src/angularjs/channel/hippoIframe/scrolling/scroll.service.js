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

  constructor(ScalingService, BrowserService) {
    'ngInject';

    this.ScalingService = ScalingService;
    this.BrowserService = BrowserService;
  }

  init(iframe) {
    this.iframe = iframe;
    this.enabled = false;
  }

  enable(scrollAllowed) {
    if (!this.enabled && this.iframe) {
      this.iframeDocument = this.iframe.contents();
      this.scrollable = this.iframeDocument.find('html, body');
      this.iframeBody = this.iframeDocument.find('body');

      if (this.BrowserService.isFF()) {
        this.bindMouseMove(scrollAllowed);
      } else {
        this.bindMouseEnterMouseLeave(scrollAllowed);
      }
      this.enabled = true;
    }
  }

  disable() {
    if (this.enabled && this.iframe) {
      this.stopScrolling();
      if (this.BrowserService.isFF()) {
        this.unbindMouseMove();
      } else {
        this.unbindMouseEnterMouseLeave();
      }
      this.enabled = false;
    }
  }

  bindMouseEnterMouseLeave(scrollAllowed) {
    this.iframe
      .on(`mouseenter${EVENT_NAMESPACE}`, () => this.stopScrolling())
      .on(`mouseleave${EVENT_NAMESPACE}`, (event) => {
        if (scrollAllowed()) {
          this.startScrolling(event.pageX, event.pageY);
        }
      });
  }

  unbindMouseEnterMouseLeave() {
    this.iframe.off(EVENT_NAMESPACE);
  }

  bindMouseMove(scrollAllowed) {
    const { iframeTop, iframeBottom } = this._getIframeCoords();
    const upperBound = 0;
    const bottomBound = iframeBottom - iframeTop;
    let mouseHasLeft = false;

    this.iframeDocument.on(`mousemove${EVENT_NAMESPACE}`, (event) => {
      if (scrollAllowed()) {
        // event pageX&Y coordinates are relative to the iframe, but expected to be relative to the NG app.
        const bodyScrollTop = this._getBodyScrollTop();
        const pageY = event.pageY - bodyScrollTop;

        if (mouseHasLeft) {
          if (pageY > upperBound && pageY < bottomBound) {
            this.stopScrolling();
            mouseHasLeft = false;
          }
        } else if (pageY <= upperBound || pageY >= bottomBound) {
          mouseHasLeft = true;
          this.startScrolling(event.pageX, pageY + iframeTop);
        }
      }
    });
  }

  unbindMouseMove() {
    this.iframeDocument.off(EVENT_NAMESPACE);
  }

  startScrolling(mouseX, mouseY) {
    const { iframeHeight, iframeTop, iframeBottom } = this._getIframeCoords();
    const bodyScrollTop = this._getBodyScrollTop();

    let scrollTop = 0;
    let distance = 0;

    if (mouseY <= iframeTop) {
      // scroll to top
      distance = bodyScrollTop;
    } else if (mouseY >= iframeBottom) {
      // scroll to bottom
      scrollTop = this.iframeBody[0].scrollHeight - iframeHeight;
      distance = scrollTop - bodyScrollTop;
    }

    if (distance > 0) {
      const duration = this._calculateDuration(distance, DURATION_MIN, DURATION_MAX);
      this._scroll(scrollTop, duration);
    }
  }

  stopScrolling() {
    if (this.scrollable) {
      this.scrollable.stop();
    }
  }

  _scroll(scrollTop, duration) {
    this.scrollable.stop().animate({ scrollTop }, {
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

  // IE and FireFox with mousemove always return zero for body.scrollTop(), so check html as well
  _getBodyScrollTop() {
    return (this.iframeDocument[0].documentElement && this.iframeDocument[0].documentElement.scrollTop) ||
            this.iframeBody.scrollTop();
  }

  _getIframeCoords() {
    const iframeWidth = this.iframe.outerWidth();
    const iframeHeight = this.iframe.outerHeight();
    const iframeOffset = this.iframe.offset();

    return {
      iframeWidth,
      iframeHeight,
      iframeTop: iframeOffset.top,
      iframeRight: iframeOffset.left + iframeWidth,
      iframeBottom: iframeOffset.top + iframeHeight,
      iframeLeft: iframeOffset.left,
    };
  }
}

export default ScrollService;
