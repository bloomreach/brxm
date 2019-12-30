/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

interface Coordinates {
  x: number;
  y: number;
}

/**
 * Normalize the amount of pixels to scroll from a wheel event as Firefox and Chrome have different implementations.
 * A single 'tick' of the scroll-wheel amounts for approximately 40 pixels.
 *
 * This coded is a simplified version of:
 * https://github.com/facebookarchive/fixed-data-table/blob/master/src/vendor_upstream/dom/normalizeWheel.js
 */
export function normalizeWheelEvent(event: any): Coordinates {
  if (event.deltaMode === 1) { // Firefox
    return {
      x: event.deltaX * 40,
      y: event.deltaY * 40,
    };
  }

  return {
    x : event.wheelDeltaX === 0 ? 0 : event.wheelDeltaX / -3,
    y : event.wheelDeltaY === 0 ? 0 : event.wheelDeltaY / -3,
  };
}
