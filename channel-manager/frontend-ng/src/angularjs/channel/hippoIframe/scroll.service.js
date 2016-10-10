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

const EVENT_NAMESPACE = '.scroll-events';
const DURATION_MIN = 500;
const DURATION_MAX = 1500;

class ScrollService {

  constructor(ScalingService, deviceDetector, BROWSERS) {
    'ngInject';

    this.ScalingService = ScalingService;
    const browsersWithNativeSupport = [BROWSERS.CHROME, BROWSERS.MS_EDGE, BROWSERS.OPERA];
    this._hasNativeSupport = browsersWithNativeSupport.indexOf(deviceDetector.browser) > -1;
  }

  init(el, container, easing = 'ease-in-out') {
    this.el = el;
    this.container = container;
    this.easing = easing;
  }

  enable(scrollAllowed = () => true) {
    if (this._hasNativeSupport) {
      return;
    }

    this.container
      .on(`mouseenter${EVENT_NAMESPACE}`, () => this.stopScrolling())
      .on(`mouseleave${EVENT_NAMESPACE}`, (data) => {
        if (scrollAllowed()) {
          this.startScrolling(data.pageX, data.pageY);
        }
      });
  }

  disable() {
    if (this._hasNativeSupport) {
      return;
    }

    this.stopScrolling();
    if (this.container) {
      this.container.off(EVENT_NAMESPACE);
    }
  }

  startScrolling(mouseX, mouseY) {
    const containerOffset = this.container.offset();
    const containerHeight = this.container.outerHeight();
    const containerScrollTop = this.container.scrollTop();
    const containerTop = containerOffset.top;
    const containerBottom = containerTop + containerHeight;

    let offset = 0;
    if (mouseY < containerTop) {
      // scroll up to top position
      offset = -containerScrollTop;
    } else if (mouseY >= containerBottom) {
      // scroll down to the bottom position
      const contentHeight = this.el.outerHeight() * this.ScalingService.getScaleFactor();
      offset = (contentHeight - containerHeight) - containerScrollTop;
    }

    if (offset) {
      this._scroll(offset);
    }
  }

  stopScrolling() {
    if (this.el) {
      this.el.velocity('stop', 'autoscroll');
    }
  }

  _scroll(offset) {
    this.el
      .velocity('stop', 'autoscroll')
      .velocity('scroll', {
        container: this.container,
        duration: this._calculateDuration(offset),
        easing: this.easing,
        offset,
        queue: 'autoscroll',
      }).dequeue('autoscroll');
  }


  _calculateDuration(distance) {
    distance = Math.abs(distance);
    const duration = distance * 2;
    return Math.min(Math.max(DURATION_MIN, duration), DURATION_MAX);
  }
}

export default ScrollService;
