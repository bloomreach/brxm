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
import axios from 'axios';
import { NextPageContext } from 'next';
// tslint:disable-next-line:import-name
import getConfig from 'next/config';
import { BrComponent, BrPage, BrPageContext } from '@bloomreach/react-sdk';
import { Banner, Content, Menu, NewsList } from '../components';

const { publicRuntimeConfig } = getConfig();

interface IndexProps {
  request: any;
}

export default class Index extends React.Component<IndexProps> {
  static async getInitialProps({ req: request }: NextPageContext) {
    return { request };
  }

  render() {
    const configuration = {
      httpClient: axios.request,
      options: {
        live: {
          cmsBaseUrl: publicRuntimeConfig.liveBrBaseUrl,
          spaBaseUrl: publicRuntimeConfig.liveSpaBaseUrl,
        },
        preview: {
          cmsBaseUrl: publicRuntimeConfig.previewBrBaseUrl,
          spaBaseUrl: publicRuntimeConfig.previewSpaBaseUrl,
        },
      },
      request: this.props.request,
    };
    const mapping = { Banner, Content, 'News List': NewsList };

    return (
      <BrPage configuration={configuration} mapping={mapping}>
        <header>
          <nav className="navbar navbar-expand-sm navbar-dark sticky-top bg-dark" role="navigation">
            <div className="container">
              <BrPageContext.Consumer>
                { page => (
                  <a href={page!.getUrl('/')} className="navbar-brand">{ page!.getTitle() || 'Server-Side React Demo'}</a>
                ) }
              </BrPageContext.Consumer>
              <div className="collapse navbar-collapse">
                <BrComponent path="menu">
                  <Menu />
                </BrComponent>
              </div>
            </div>
          </nav>
        </header>
        <section className="container pt-3">
          <BrComponent path="main" />
        </section>
        <footer className="bg-dark text-light py-3">
          <div className="container">
            <div className="float-left pr-3">&copy; Bloomreach</div>
            <div className="overflow-hidden">
              <BrComponent path="footer" />
            </div>
          </div>
        </footer>
      </BrPage>
    );
  }
}
