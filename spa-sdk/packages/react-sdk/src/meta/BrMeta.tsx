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
import { MetaCollection } from '@bloomreach/spa-sdk';

interface BrMetaProps {
  meta: MetaCollection;
}

export class BrMeta extends React.Component<BrMetaProps> {
  private headRef = React.createRef<HTMLElement>();

  private tailRef = React.createRef<HTMLElement>();

  componentDidMount() {
    this.renderMeta();
  }

  componentDidUpdate(prevProps: BrMetaProps) {
    prevProps.meta.clear();

    this.renderMeta();
  }

  componentWillUnmount() {
    this.props.meta.clear();
  }

  private renderMeta() {
    const head = this.headRef?.current?.nextSibling;
    const tail = this.tailRef?.current;

    if (!head || !tail) {
      return;
    }

    this.props.meta.render(head, tail);
  }

  render() {
    return (
      <>
        {this.props.meta.length > 0 && <span style={{ display: 'none' }} ref={this.headRef} />}
        {this.props.children}
        {this.props.meta.length > 0 && <span style={{ display: 'none' }} ref={this.tailRef} />}
      </>
    );
  }
}
