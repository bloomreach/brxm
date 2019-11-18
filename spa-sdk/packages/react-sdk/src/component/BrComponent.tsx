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
import { BrComponentContext } from './BrComponentContext';
import { BrNode } from './BrNode';

interface BrComponentProps {
  /**
   * The path to a component.
   * The path is defined as a slash-separated components name chain
   * relative to the current component (e.g. `main/container`).
   * If it is omitted, all the children will be rendered.
   */
  path?: string;
}

/**
 * The brXM component.
 */
export class BrComponent extends React.Component<BrComponentProps> {
  static contextType = BrComponentContext;
  context: React.ContextType<typeof BrComponentContext>;

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
    return this.getComponents().map((component, index) => (
      <BrNode key={index} component={component}>{this.props.children}</BrNode>
    ));
  }

  render() {
    return <>{this.renderComponents()}</>;
  }
}
