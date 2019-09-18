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

import { Content } from './content';

/**
 * The map holding content objects.
 */
export interface ContentMap extends Map<string, Content> {
  /**
   * Gets a content item by a reference.
   * @param reference The reference to a content item.
   */
  get(reference: string): Content | undefined;

  /**
   * Puts a content item under a reference.
   * @param reference The reference to a content item.
   * @param content The content.
   */
  set(reference: string, content: Content): this;
}
