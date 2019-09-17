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

 import { BrPage } from '@bloomreach/react-sdk';

 const BR_ORIGIN = new URL(process.env.REACT_APP_BR_ORIGIN!);
 const BR_CONTEXT_PATH = process.env.REACT_APP_BR_CONTEXT_PATH;
 const BR_CHANNEL_PATH = process.env.REACT_APP_BR_CHANNEL_PATH;

 const urlConfig = {
   scheme: BR_ORIGIN.protocol.slice(0, -1),
   hostname: BR_ORIGIN.hostname,
   port: BR_ORIGIN.port,
   contextPath: BR_CONTEXT_PATH,
   channelPath: BR_CHANNEL_PATH
 };

 const cmsUrls = {
   preview: urlConfig,
   live: urlConfig
 };

const App: React.FC = () => {
  return (
    <BrPage>
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
        cmsUrls: <pre>{JSON.stringify(cmsUrls, null, 2)}</pre>
      </div>
    </BrPage>
  );
};

export default App;
