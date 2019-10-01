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

import React from 'react';
import {
  Container,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from '@bloomreach/spa-sdk';
import { BrNodeComponent } from './BrNodeComponent';
import { BrContainerBox, BrContainerInline, BrContainerNoMarkup, BrContainerOrderedList, BrContainerUnorderedList } from '../cms';

export class BrNodeContainer extends BrNodeComponent<Container> {
  protected getMapping() {
    return this.props.component.getType();
  }

  protected fallback() {
    switch (this.props.component.getType()) {
      case TYPE_CONTAINER_INLINE: return <BrContainerInline {...this.props} />;
      case TYPE_CONTAINER_NO_MARKUP: return <BrContainerNoMarkup {...this.props} />;
      case TYPE_CONTAINER_ORDERED_LIST: return <BrContainerOrderedList {...this.props} />;
      case TYPE_CONTAINER_UNORDERED_LIST: return <BrContainerUnorderedList {...this.props} />;
      default: return <BrContainerBox {...this.props} />;
    }
  }
}
