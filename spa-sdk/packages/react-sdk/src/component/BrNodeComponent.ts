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
import { Component } from '@bloomreach/spa-sdk';
import { BrProps } from './BrProps';
import { BrMappingContext } from './BrMappingContext';

export class BrNodeComponent<T extends Component> extends React.Component<BrProps<T>> {
  static contextType = BrMappingContext;
  context!: React.ContextType<typeof BrMappingContext>;

  protected getMapping(): string | undefined {
    return this.props.component.getName();
  }

  protected fallback() {
    return this.props.children;
  }

  render() {
    const mapping = this.getMapping();
    const component = mapping && this.context[mapping];
    if (!component) {
      return this.fallback();
    }

    return React.createElement(component, this.props);
  }
}
