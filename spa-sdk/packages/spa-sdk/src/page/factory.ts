/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

export type Builder<T extends any[], U> = (...args: T) => U;

export interface Factory<T extends any[], U> {
  /**
   * Creates an instance from the given arguments.
   */
  create(...args: T): U;
}

export abstract class SimpleFactory<T, U extends Builder<any, any>> implements Factory<any, any> {
  protected mapping = new Map<T, U>();

  /**
   * Registers a builder for the specified type.
   * @param type The entity type.
   * @param builder The entity builder.
   */
  register(type: T, builder: U) {
    this.mapping.set(type, builder);

    return this;
  }

  abstract create(...args: any[]): any;
}
