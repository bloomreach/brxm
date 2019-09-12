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

export const META_POSITION_BEGIN = 'begin';
export const META_POSITION_END = 'end';

export type MetaType = string;
export type MetaPosition = typeof META_POSITION_BEGIN | typeof META_POSITION_END;

export interface MetaModel<T extends MetaType = MetaType> {
  data: string;
  type: T;
}

export interface MetaCollectionModel {
  beginNodeSpan?: MetaModel[];
  endNodeSpan?: MetaModel[];
}

/**
 * Meta information describing a part of the page.
 */
export interface Meta<T extends MetaType = MetaType> {
  /**
   * Returns meta-data.
   */
  getData(): string;

  /**
   * Returns meta-data type.
   */
  getType(): T;

  /**
   * Returns meta-data position relative to the related element.
   */
  getPosition(): MetaPosition;
}

export class Meta<T extends MetaType = MetaType> implements Meta {
  constructor(protected model: MetaModel<T>, protected position: MetaPosition) {}

  getData() {
    return this.model.data;
  }

  getType() {
    return this.model.type;
  }

  getPosition() {
    return this.position;
  }
}
