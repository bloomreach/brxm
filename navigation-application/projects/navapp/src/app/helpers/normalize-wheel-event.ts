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

const LINE_HEIGHT = 40;

interface NormalizedWheelData {
  pixelX: number;
  pixelY: number;
}

export function normalizeWheelEvent(event: any): NormalizedWheelData {
  if (event.deltaMode === 1) { // Firefox
    return {
      pixelX: event.deltaX * LINE_HEIGHT,
      pixelY: event.deltaY * LINE_HEIGHT,
    };
  } else {
    return {
      pixelX : event.wheelDeltaX === 0 ? 0 : event.wheelDeltaX / -3,
      pixelY : event.wheelDeltaY === 0 ? 0 : event.wheelDeltaY / -3,
    };
  }
}
