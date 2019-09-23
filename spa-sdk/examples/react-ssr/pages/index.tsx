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
import { BrPage } from '@bloomreach/react-sdk';

const { publicRuntimeConfig } = getConfig();
const config = {
  httpClient: axios.request,
  options: {
    live: {
      pageModelBaseUrl: publicRuntimeConfig.brUrlLive,
      spaBasePath: publicRuntimeConfig.spaBasePathLive,
    },
    preview: {
      pageModelBaseUrl: publicRuntimeConfig.brUrlPreview,
      spaBasePath: publicRuntimeConfig.spaBasePathPreview,
    },
  },
};

interface IndexProps {
  request: any;
}

interface IndexState {}

export default class Index extends React.Component<IndexProps, IndexState> {
  static async getInitialProps({ req: request }: NextPageContext) {
    return { request };
  }

  render() {
    return (
      <BrPage configuration={{ ...config, request: this.props.request }} mapping={{}}>
        <div id='header'>
          <nav className='navbar navbar-expand-md navbar-dark bg-dark'>
            <span className='navbar-brand'>Server-side React Demo</span>
            <button className='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbar-collapse'
                    aria-controls='navbar-collapse' aria-expanded='false' aria-label='Toggle navigation'>
              <span className='navbar-toggler-icon' />
            </button>
            <div className='collapse navbar-collapse' id='navbar-collapse'>
              Render Menu
            </div>
          </nav>
        </div>
        <div className='container marketing'>
          Render Container here<br/>
          cmsUrls: <pre>{JSON.stringify(config, null, 2)}</pre>
          request: <pre>{JSON.stringify(this.props.request, null, 2)}</pre>
        </div>
      </BrPage>
    );
  }
}
