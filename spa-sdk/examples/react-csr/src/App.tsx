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

const App: React.FC = () => {
  return (
    <div id="br-page">
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
            Render Menu
          </div>
        </nav>
      </div>
      <div className="container marketing">
        Render Container
      </div>
    </div>
  );
}

export default App;
