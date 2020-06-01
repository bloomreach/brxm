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

export const TYPE_META_COMMENT = 'comment';

/**
 * Meta-data following before a page component.
 */
export const META_POSITION_BEGIN = 'begin';

/**
 * Meta-data following after a page component.
 */
export const META_POSITION_END = 'end';

export type MetaType = typeof TYPE_META_COMMENT;
export type MetaPosition = typeof META_POSITION_BEGIN | typeof META_POSITION_END;

/**
 * @hidden
 */
export interface MetaModel {
  data: string;
  type: MetaType;
}

/**
 * Meta information describing a part of the page.
 */
export interface Meta {
  /**
   * @return The meta-data.
   */
  getData(): string;

  /**
   * @return The meta-data position relative to the related element.
   */
  getPosition(): MetaPosition;
}

export class MetaImpl implements Meta {
  constructor(protected model: MetaModel, protected position: MetaPosition) {}

  getData() {
    return this.model.data;
  }

  getPosition() {
    return this.position;
  }
}

/**
 * Checks whether a value is a meta-data object.
 * @param value The value to check.
 */
export function isMeta(value: any): value is Meta {
  return value instanceof MetaImpl;
}
