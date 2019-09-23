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

import { destroy, initialize, Page } from '@bloomreach/spa-sdk';

interface ComponentMapping {
  [key: string]: typeof React.Component;
}

interface BrPageProps {
  configuration: any;
  mapping?: ComponentMapping;
}

interface BrPageState {
  page: Page;
}

export class BrPage extends React.Component<BrPageProps, BrPageState> {
  private cms: any; // TODO: replace with real CMS

  async componentDidMount() {
    if (!this.state || !this.state.page) {
      const page = await initialize(this.props.configuration);
      this.setState({ page });
    }
  }

  async componentDidUpdate(prevProps: BrPageProps, prevState: BrPageState) {
    const config = this.props.configuration;
    if (config.request.path !== prevProps.configuration.request.path) {
      if (this.state.page) {
        destroy(this.state.page);
      }

      const page = await initialize(config);
      this.setState({ page });
    }

    const prevPage = prevState && prevState.page || null;
    if (this.state.page !== prevPage && this.cms) {
      this.cms.createOverlay();
    }
  }

  render () {
    return (
      <div className="br-page">{this.props.children}</div>
    );
  }
}
