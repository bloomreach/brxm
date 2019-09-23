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
import { shallow } from 'enzyme';
import { initialize, TYPE_COMPONENT } from '@bloomreach/spa-sdk';

import { BrPage, ComponentMap } from './BrPage';

const model = {
  page: {
    type: TYPE_COMPONENT, name: 'page1'
  },
};

const config = {
  httpClient: jest.fn(async () => model),
  request: { path: '/' },
  options: {
    live: {
      pageModelBaseUrl: 'http://localhost:8080/site/my-spa',
    },
  },
};

describe('BrPage', function() {
  it('should accept config', function() {
    const wrap = shallow(<BrPage configuration={config}/>);
    expect(wrap.contains(<div className="br-page"></div>)).toBe(true);
  });

  it('should accept a mapping', function() {
    class News extends React.Component {}
    const mapping = { 'News': News };
    const wrap = shallow(<BrPage configuration={config} mapping={mapping}/>);
    expect(wrap.contains(<div className="br-page"></div>)).toBe(true);
  });

  it('should render children', function() {
    const wrap = shallow(<BrPage configuration={config}>br-page-children</BrPage>);
    expect(wrap.contains(<div className="br-page">br-page-children</div>)).toBe(true);
  });
});
