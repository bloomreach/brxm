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
import { isComponent, Component } from '@bloomreach/spa-sdk';
import { BrComponentContext } from './BrComponentContext';
import { BrNode } from './BrNode';

interface BrComponentProps {
  path?: string;
}

interface BrComponentState {
  components: Component[];
}

export class BrComponent extends React.Component<BrComponentProps, BrComponentState> {
  static contextType = BrComponentContext;

  constructor(props: BrComponentProps, public context: React.ContextType<typeof BrComponentContext>) {
    super(props, context);

    this.state = {
      components: this.getComponents(),
    };
  }

  componentDidUpdate(prevProps: BrComponentProps) {
    if (this.props.path !== prevProps.path) {
      this.setState({ components: this.getComponents() });
    }
  }

  private getComponents() {
    if (!this.context) {
      return [];
    }
    if (!this.props.path) {
      return this.context.getChildren();
    }

    const component = this.context.getComponent(...this.props.path.split('/'));

    return component ? [component] : [];
  }

  private renderComponents() {
    return this.state.components.map((component, index) => (
      <BrNode key={index} component={component}>{this.props.children}</BrNode>
    ));
  }

  render() {
    return <>{this.renderComponents()}</>;
  }
}
