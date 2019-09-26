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

import * as spaSdk from '@bloomreach/spa-sdk';

import React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';

import { BrPage } from './BrPage';
import { Meta } from '../meta/Meta';

const model = {
  data: {
    page: {
      id: 'page1',
      type: 'COMPONENT',
      name: 'Page1',
      "_meta": {
        "beginNodeSpan": [
          {
            "type": "comment",
            "data": "comment-start"
          },
        ],
        "endNodeSpan": [
          {
            "type": "comment",
            "data": "comment-end"
          },
        ],
      },
    },
  }
};

const config = {
  httpClient: jest.fn(async () => model),
  request: { path: '/' },
  options: {
    live: {
      pageModelBaseUrl: 'http://localhost:8080/site/my-spa/resourceapi',
    },
    preview: {
      pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa/resourceapi',
      spaBasePath: '/site/_cmsinternal/my-spa',
    },
  },
};

class TestComponent extends React.Component {}
const mapping = { TestComponent };

describe('BrPage', function() {
  let spaInit: any;
  let spaDestroy: any;

  beforeEach(() => {
    spaInit = jest.spyOn(spaSdk, 'initialize');
    spaDestroy = jest.spyOn(spaSdk, 'destroy');
  });

  afterEach(() => {
    spaInit.mockRestore();
    spaDestroy.mockRestore();
  });

  async function createBrPage(children: JSX.Element | undefined = undefined): Promise<ShallowWrapper> {
    const wrapper = shallow(
      <BrPage configuration={config} mapping={mapping}>
        {children}
      </BrPage>
    );
    // wait for BrPage.initializePage to resolve
    const page = await spaInit.mock.results[0].value;

    return wrapper;
  }

  it('should initialize the SPA SDK', async () => {
    const wrapper = await createBrPage();
    expect(spaInit).toHaveBeenCalledWith(config);

    const page: spaSdk.Page = wrapper.state('page');
    expect(page).toBeDefined();
    expect(page.getComponent().getId()).toBe('page1');
  });

  it('should render nothing if there is no page', async () => {
    spaInit.mockReturnValue(null);
    const wrapper = await createBrPage();

    expect(wrapper.isEmptyRender()).toBe(true);
  });

  it('should render nothing if there is an error loading the page', (done) => {
    spaInit.mockRejectedValue('error-loading-page');
    const wrapper = shallow(<BrPage configuration={config} mapping={{}}/>);
    spaInit.mock.results[0].value.catch((e: Error) => {
      expect(wrapper.isEmptyRender()).toBe(true);
      expect(e).toEqual('error-loading-page');
      done();
    });
  });

  it('should render MappingContext.provider', async () => {
    const wrapper = await createBrPage();

    expect(wrapper.find("ContextProvider").first().prop('value')).toEqual(mapping);
  });

  it('should render PageContext.provider', async () => {
    const wrapper = await createBrPage();
    const page = wrapper.state('page');

    expect(wrapper.find("ContextProvider").last().prop('value')).toEqual(page);
  });

  it('should render children', async () => {
    const wrapper = await createBrPage(<div id="br-page-child"></div>);
    await spaInit.mock.results[0].value;

    expect(wrapper.contains([
      <div id="br-page-child"></div>
    ])).toBe(true);
  });

  it('should render meta data in comments', async () => {
    const wrapper = await createBrPage();
    const page: spaSdk.Page = wrapper.state('page');
    const pageMeta = page.getComponent().getMeta();

    expect(wrapper.contains(<Meta meta={pageMeta[0]} />)).toBe(true);
    expect(wrapper.contains(<Meta meta={pageMeta[1]} />)).toBe(true);
  });

  it('should destroy current page and init new page when configuration changes', async () => {
    const wrapper = await createBrPage();
    const page = wrapper.state('page');
    const newConfig = { ...config };
    wrapper.setProps({
      configuration: newConfig,
    });

    expect(spaDestroy).toHaveBeenCalledWith(page);
    expect(spaInit).toHaveBeenCalledWith(newConfig);
  });

  it('should destroy the page when unmounting', async () => {
    const wrapper = await createBrPage();
    const page = wrapper.state('page');

    wrapper.unmount();
    expect(spaDestroy).toHaveBeenCalledWith(page);
  });

  it('should not destroy a null page when unmounting', async () => {
    spaInit.mockReturnValue(null);
    const wrapper = await createBrPage();

    wrapper.unmount();
    expect(spaDestroy).not.toHaveBeenCalled();
  });
});
