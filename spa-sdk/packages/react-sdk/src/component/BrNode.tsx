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

import React from 'react';
import { isContainer, isContainerItem, Component } from '@bloomreach/spa-sdk';
import { BrMeta } from '../meta';
import { BrPageContext } from '../page/BrPageContext';
import { BrComponentContext } from './BrComponentContext';
import { BrNodeContainer } from './BrNodeContainer';
import { BrNodeContainerItem } from './BrNodeContainerItem';
import { BrNodeComponent } from './BrNodeComponent';

interface BrNodeProps {
  component: Component;
}

export class BrNode extends React.Component<BrNodeProps> {
  static contextType = BrPageContext;
  context: React.ContextType<typeof BrPageContext>;

  private renderNode() {
    if (isContainer(this.props.component)) {
      return (
        <BrNodeContainer component={this.props.component} page={this.context!}>
          {this.renderChildren()}
        </BrNodeContainer>
      );
    }

    if (isContainerItem(this.props.component)) {
      return (
        <BrNodeContainerItem component={this.props.component} page={this.context!}>
          {this.renderChildren()}
        </BrNodeContainerItem>
      );
    }

    return (
      <BrNodeComponent component={this.props.component} page={this.context!}>
        {this.renderChildren()}
      </BrNodeComponent>
    );
  }

  private renderChildren() {
    return this.props.children
      ?? this.props.component.getChildren()
        .map((child, index) => <BrNode key={index} component={child} />);
  }

  render() {
    return(
      <BrComponentContext.Provider value={this.props.component}>
        <BrMeta meta={this.props.component.getMeta()}>
          {this.renderNode()}
        </BrMeta>
      </BrComponentContext.Provider>
    );
  }
}
