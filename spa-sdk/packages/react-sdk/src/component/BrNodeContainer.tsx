/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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

import {
  Container,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from '@bloomreach/spa-sdk';
import { BrNodeComponent } from './BrNodeComponent';
import { BrProps } from './BrProps';
import { BrContainerBox, BrContainerInline, BrContainerNoMarkup, BrContainerOrderedList, BrContainerUnorderedList } from '../cms';

export class BrNodeContainer extends BrNodeComponent<Container> {
  protected getMapping(): React.ComponentType<BrProps> {
    const type = this.props.component.getType();

    if (type && type in this.context) {
      return this.context[type] as React.ComponentType<BrProps>;
    }

    switch (type) {
      case TYPE_CONTAINER_INLINE: return BrContainerInline;
      case TYPE_CONTAINER_NO_MARKUP: return BrContainerNoMarkup;
      case TYPE_CONTAINER_ORDERED_LIST: return BrContainerOrderedList;
      case TYPE_CONTAINER_UNORDERED_LIST: return BrContainerUnorderedList;
      default: return BrContainerBox;
    }
  }
}
