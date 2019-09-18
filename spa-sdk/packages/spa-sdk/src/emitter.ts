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

type Constructor = new (...args: any) => any;

export type Emitter<T> = Pick<Typed<T>, 'on' | 'off'>;

// tslint:disable-next-line:function-name variable-name
export function EmitterMixin<T extends Events>(Super: Constructor) {
  return class EmitterMixin extends Super implements Emitter<T> {
    private emitter = new Typed<T>();

    on = this.emitter.on.bind(this.emitter);
    off = this.emitter.off.bind(this.emitter);
    protected emit = this.emitter.emit.bind(this.emitter);
  };
}
