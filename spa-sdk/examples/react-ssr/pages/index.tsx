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

import { NextPage } from 'next';
import getConfig from 'next/config';

import { sdk } from '@bloomreach/react-sdk';

// TODO: Here to validate the setup
sdk();

const { publicRuntimeConfig } = getConfig();
const brOrigin = new URL(publicRuntimeConfig.brOrigin);

const urlConfig = {
  scheme: brOrigin.protocol.slice(0, -1),
  hostname: brOrigin.hostname,
  port: brOrigin.port,
  contextPath: publicRuntimeConfig.brContextPath,
  channelPath: publicRuntimeConfig.brChannelPath,
};

const cmsUrls = {
  preview: urlConfig,
  live: urlConfig,
};

const componentDefinitions = {};

const Home: NextPage<{ pageModel: any }> = ({pageModel}) => (
    <div id="br-page">
      <div id='header'>
        <nav className='navbar navbar-expand-md navbar-dark bg-dark'>
          <span className='navbar-brand'>Server-side React Demo</span>
          <button className='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarCollapse'
                  aria-controls='navbarCollapse' aria-expanded='false' aria-label='Toggle navigation'>
            <span className='navbar-toggler-icon' />
          </button>
          <div className='collapse navbar-collapse' id='navbarCollapse'>
            Render Menu
          </div>
        </nav>
      </div>
      <div className='container marketing'>
        Render Container here<br/>
        cmsUrls: <pre>{JSON.stringify(cmsUrls, null, 2)}</pre>
        pageModel: <pre>{JSON.stringify(pageModel, null, 2)}</pre>
      </div>
    </div>
);

Home.getInitialProps = async ({ req, asPath }) => {
  return {
    pageModel: {
      host: req!.headers.host,
      path: asPath,
    }
  };
};

export default Home;
