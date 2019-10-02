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
import { destroy, initialize, Configuration, Page } from '@bloomreach/spa-sdk';
import { BrMappingContext, BrNode } from '../component';
import { BrPageContext } from './BrPageContext';

interface BrPageProps {
  configuration: Configuration;
  mapping: React.ContextType<typeof BrMappingContext>;
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
    try {
      const page = await initialize(this.props.configuration);
      this.setState({ page });
    } catch (error) {
      this.setState(() => { throw error; });
    }
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
      <BrPageContext.Provider value={this.state.page}>
        <BrMappingContext.Provider value={this.props.mapping}>
          <BrNode component={this.state.page.getComponent()}>
            {this.props.children}
          </BrNode>
        </BrMappingContext.Provider>
      </BrPageContext.Provider>
    );
  }
}
