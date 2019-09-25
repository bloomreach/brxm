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
import { initialize, destroy, Configuration, Page, META_POSITION_BEGIN, META_POSITION_END } from '@bloomreach/spa-sdk';
import { Meta } from '../meta';
import { MappingContext } from './MappingContext';
import { PageContext } from './PageContext';

interface BrPageProps {
  configuration: Configuration;
  mapping: React.ContextType<typeof MappingContext>;
}

interface BrPageState {
  page?: Page;
}

export class BrPage extends React.Component<BrPageProps, BrPageState> {
  constructor(props: BrPageProps) {
    super(props);

    this.state = {};
  }

  componentDidMount() {
    this.initializePage();
  }

  componentDidUpdate(prevProps: BrPageProps, prevState: BrPageState) {
    if (this.props.configuration !== prevProps.configuration) {
      this.destroyPage();
      this.initializePage();
    }

    if (this.state.page && this.state.page !== prevState.page) {
      this.state.page.sync();
    }
  }

  componentWillUnmount() {
    this.destroyPage();
  }

  private async initializePage() {
    const page = await initialize(this.props.configuration);
    this.setState({ page });
  }

  private destroyPage() {
    if (!this.state.page) {
      return;
    }

    destroy(this.state.page);
  }

  render () {
    if (!this.state.page) {
      return null;
    }

    return (
      <MappingContext.Provider value={this.props.mapping}>
        <PageContext.Provider value={this.state.page}>
          {this.renderMeta(META_POSITION_BEGIN)}
          {this.props.children}
          {this.renderMeta(META_POSITION_END)}
        </PageContext.Provider>
      </MappingContext.Provider>
    );
  }

  private renderMeta(position: typeof META_POSITION_BEGIN | typeof META_POSITION_END) {
    return this.state.page!.getComponent()
      .getMeta()
      .filter(meta => position === meta.getPosition())
      .map((meta, index) => <Meta key={index} meta={meta} />);
  }
}
