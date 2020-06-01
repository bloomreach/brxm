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
import { Configuration, PageModel, Page, destroy, initialize, isPage } from '@bloomreach/spa-sdk';
import { BrMappingContext, BrNode } from '../component';
import { BrPageContext } from './BrPageContext';

interface BrPageProps {
  /**
   * The configuration of the SPA SDK.
   * @see https://www.npmjs.com/package/@bloomreach/spa-sdk#configuration
   */
  configuration: Configuration;

  /**
   * The brXM and React components mapping.
   */
  mapping: React.ContextType<typeof BrMappingContext>;

  /**
   * The pre-initialized page instance or prefetched page model.
   * Mostly this property should be used to transfer state from the server-side to the client-side.
   */
  page?: Page | PageModel;
}

interface BrPageState {
  page?: Page;
}

/**
 * @typedef {Object} BrPageProps
 * @property {Configuration} configuration The configuration of the SPA SDK.
 * @property {Object} mapping The brXM and React components mapping.
 * @property {Page | PageModel | undefined} page The pre-initialized page instance or prefetched page model.
 * Mostly this property should be used to transfer state from the server-side to the client-side.
 */

/**
 * The brXM page.
 */
export class BrPage extends React.Component<BrPageProps, BrPageState> {
  /**
   * @param props {BrPageProps}
   */
  constructor(props: BrPageProps) {
    super(props);

    this.state = {};
  }

  componentDidMount() {
    this.initializePage();
  }

  componentDidUpdate(prevProps: BrPageProps, prevState: BrPageState) {
    if (this.props.configuration !== prevProps.configuration || this.props.page !== prevProps.page) {
      this.destroyPage();
      this.initializePage(this.props.page === prevProps.page);
    }

    if (this.state.page !== prevState.page) {
      this.forceUpdate(() => this.state.page?.sync());
    }
  }

  componentWillUnmount() {
    this.destroyPage();
  }

  private async initializePage(force = false) {
    const page = force ? undefined : this.props.page;

    if (isPage(page)) {
      return this.setState({ page });
    }

    try {
      this.setState({ page: await initialize(this.props.configuration, page) });
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
