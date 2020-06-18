/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
  it('detects and normalizes Firefox mouse wheel events', () => {
    expect(normalizeWheelEvent({deltaMode: 1, deltaX: 0, deltaY: 0 }))
      .toEqual({ x: 0, y: 0, wheel: true });
    expect(normalizeWheelEvent({deltaMode: 1, deltaX: -1, deltaY: 2 }))
      .toEqual({ x: -40, y: 80, wheel: true });
  });

  it('detects Chrome mouse wheel events', () => {
    expect(normalizeWheelEvent({wheelDeltaY: -120, deltaX: 0, deltaY: 0 }))
      .toEqual({ x: 0, y: 0, wheel: true });
    expect(normalizeWheelEvent({wheelDeltaY: 120, deltaX: -10, deltaY: 20 }))
      .toEqual({ x: -10, y: 20, wheel: true });
  });

  it('returns trackpad wheel events as is', () => {
    expect(normalizeWheelEvent({deltaMode: 0, wheelDeltaY: 0, deltaX: 0, deltaY: 0 }))
      .toEqual({ x: 0, y: 0, wheel: false });
    expect(normalizeWheelEvent({deltaMode: 0, wheelDeltaY: 10, deltaX: 10, deltaY: -20 }))
      .toEqual({ x: 10, y: -20, wheel: false });
  });
});
