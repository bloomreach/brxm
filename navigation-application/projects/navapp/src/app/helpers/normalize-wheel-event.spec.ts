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

import { normalizeWheelEvent } from './normalize-wheel-event';

describe('Normalize wheel events', () => {
  it('normalizes Firefox wheel events', () => {
    expect(normalizeWheelEvent({deltaMode: 1, deltaX: 0, deltaY: 0 })).toEqual({ pixelX: 0, pixelY: 0 });
    expect(normalizeWheelEvent({deltaMode: 1, deltaX: 0, deltaY: 1 })).toEqual({ pixelX: 0, pixelY: 40 });
    expect(normalizeWheelEvent({deltaMode: 1, deltaX: 0, deltaY: -2 })).toEqual({ pixelX: 0, pixelY: -80 });
  });

  it('normalizes Chrome/Safari wheel events', () => {
    expect(normalizeWheelEvent({deltaMode: 0, wheelDeltaX: 0, wheelDeltaY: 0 })).toEqual({ pixelX: 0, pixelY: 0 });
    expect(normalizeWheelEvent({deltaMode: 0, wheelDeltaX: 0, wheelDeltaY: -120 })).toEqual({ pixelX: 0, pixelY: 40 });
    expect(normalizeWheelEvent({deltaMode: 0, wheelDeltaX: 0, wheelDeltaY: 240 })).toEqual({ pixelX: 0, pixelY: -80 });
  });
});
