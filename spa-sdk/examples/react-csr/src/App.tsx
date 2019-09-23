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
import { RouteComponentProps } from 'react-router-dom';
import { BrPage } from '@bloomreach/react-sdk';

const config = {
  httpClient: axios.request,
  options: {
    live: {
      pageModelBaseUrl: process.env.REACT_APP_BR_URL_LIVE!,
      spaBasePath: process.env.REACT_APP_SPA_BASE_PATH_LIVE,
    },
    preview: {
      pageModelBaseUrl: process.env.REACT_APP_BR_URL_PREVIEW!,
      spaBasePath: process.env.REACT_APP_SPA_BASE_PATH_PREVIEW,
    },
  },
};

export default function App(props: RouteComponentProps) {
  return (
      <BrPage configuration={{
        ...config,
        request: {
          path: `${props.location.pathname}${props.location.search}`
        },
      }} mapping={{}}>
        <div id="header">
          <nav className="navbar navbar-expand-md navbar-dark bg-dark">
            <span className="navbar-brand">Client-side React Demo</span>
            <button type="button"
              className="navbar-toggler"
              data-toggle="collapse"
              data-target="#navbarCollapse"
              aria-controls="navbarCollapse"
              aria-expanded="false"
              aria-label="Toggle navigation">
              <span className="navbar-toggler-icon" />
            </button>
            <div className="collapse navbar-collapse" id="navbarCollapse">
              Render Menu here
            </div>
          </nav>
        </div>
        <div className="container marketing">
          Render Container here<br/>
          cmsUrls: <pre>{JSON.stringify(config, null, 2)}</pre>
        </div>
    </BrPage>
  );
}
