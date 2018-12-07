/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
  const imgHeight = 493;
  const imgWidth = 703;
  const markers = [[-19, -104]];
  const radius = 8;

  let bg;
  let current = null;

  function inRange(nr, min, max) {
    return ((nr-min)*(nr-max) <= 0);
  }

  function isOnPoint(x, y) {
    return inRange(x - current.x, -radius, radius) && inRange(y - current.y, -radius, radius);
  }

  function isLast() {
    return current.index === markers.length - 1;
  }

  function setPosition() {
    const marker = $('<span></span>');
    bg.append(marker);

    const point = markers[current.index];
    const ratio = Math.min(bg.outerHeight() / imgHeight, bg.outerWidth() / imgWidth);
    const markerPos = marker.position();
    current.x = markerPos.left + (point[0] * ratio);
    current.y = markerPos.top - (point[1] * ratio);

    marker.remove();
  }

  function next() {
    if (current === null) {
      current = { index: 0 };
    } else {
      current.index++;
    }
    setPosition();
  }

  function update() {
    if (current !== null) {
      setPosition();
    }
  }

  function ready() {
    bg = $('.login-background > div');
    bg.click(click);
    next();
  }

  function click(e) {
    if (isOnPoint(e.offsetX, e.pageY)) {
      if (isLast()) {
        $('.login-wrap').addClass('login-hi');
        setTimeout(function() {
          $('.login-wrap').removeClass('login-hi');
        }, 2000);
        current = null;
      }
      next();
    }
  }

  $(document).ready(ready);

  let resizeTimeout;
  $(window).on('resize orientationChanged', function() {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(update, 200);
  });
})();
