/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Events, Typed } from 'emittery';

type Constructor = new (...args: any[]) => any;

/**
 * Event listener.
 */
type Listener<T, U extends Extract<keyof T, string>> = (eventData: T[U]) => any;

/**
 * Function to unsubscribe a listener from an event.
 */
type UnsubscribeFn = () => void;

/**
 * Event emitter.
 */
export interface Emitter<T> {
  /**
   * Subscribes for an event.
   * @param eventName The event name.
   * @param listener The event listener.
   * @return The unsubscribe function.
   */
  on<U extends Extract<keyof T, string>>(eventName: U, listener: Listener<T, U>): UnsubscribeFn;

  /**
   * Unsubscribes from an event.
   * @param eventName The event name.
   * @param listener The event listener.
   */
  off<U extends Extract<keyof T, string>>(eventName: U, listener: Listener<T, U>): void;
}

export function EmitterMixin<T extends Constructor, U extends Events>(Super: T) {
  return class EmitterMixin extends Super implements Emitter<U> {
    /**
     * @todo should be private
     * @see https://github.com/Microsoft/TypeScript/issues/17293
     */
    /* private */ emitter = new Typed<U>();

    on = this.emitter.on.bind(this.emitter);
    off = this.emitter.off.bind(this.emitter);

    /**
     * @todo should be private
     * @see https://github.com/Microsoft/TypeScript/issues/17293
     */
    /* protected */ emit = this.emitter.emit.bind(this.emitter);
  };
}
