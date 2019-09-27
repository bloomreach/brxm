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

jest.mock('@bloomreach/spa-sdk');

import React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import { destroy, initialize, Page } from '@bloomreach/spa-sdk';

import { BrPage } from './BrPage';
import { Meta } from '../meta';
import { pageMock } from '../../__mocks__/@bloomreach/spa-sdk';

const model = {
  data: {
    page: { id: 'page1', type: 'COMPONENT' },
  },
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

const initializeMock = (initialize as jest.Mock);

async function createBrPage(children: JSX.Element | undefined = undefined): Promise<ShallowWrapper> {
  const wrapper = shallow(
    <BrPage configuration={config} mapping={mapping}>
      {children}
    </BrPage>
  );

  // wait for BrPage.initializePage to resolve
  await (initialize as jest.Mock).mock.results[0].value;
  return wrapper;
}

describe('BrPage', () => {
  beforeEach(() => {
    initializeMock.mockResolvedValue(pageMock);
  });

  afterEach(() => {
    initializeMock.mockClear();
    (destroy as jest.Mock).mockClear();
  });

  it('should initialize the SPA SDK and sync the CMS', async () => {
    const wrapper = await createBrPage();
    expect(initialize).toHaveBeenCalledWith(config);

    const page: Page = wrapper.state('page');
    expect(page).toBeDefined();
    expect(page.sync).toHaveBeenCalled();
  });

  it('should render nothing if there is no page', async () => {
    initializeMock.mockResolvedValue(null);
    const wrapper = await createBrPage();
    const page: Page = wrapper.state('page');

    expect(page).toBeNull();
    expect(wrapper.isEmptyRender()).toBe(true);
  });

  it('should render nothing if there is an error loading the page', (done) => {
    const error = new Error('error-loading-page');
    initializeMock.mockRejectedValue(error);

    const wrapper = shallow(<BrPage configuration={config} mapping={{}}/>);

    initializeMock.mock.results[0].value.catch((e: Error) => {
      expect(wrapper.isEmptyRender()).toBe(true);
      expect(e).toEqual(error);
      done();
    });
  });

  it('should render MappingContext.provider', async () => {
    const wrapper = await createBrPage();

    expect(wrapper.find('ContextProvider').first().prop('value')).toEqual(mapping);
  });

  it('should render PageContext.provider', async () => {
    const wrapper = await createBrPage();
    const page = wrapper.state('page');

    expect(wrapper.find('ContextProvider').last().prop('value')).toEqual(page);
  });

  it('should render children', async () => {
    const wrapper = await createBrPage(<div id="br-page-child"/>);

    expect(wrapper.contains(<div id="br-page-child"/>)).toBe(true);
  });

  it('should render meta data in comments', async () => {
    const wrapper = await createBrPage(<div id="child-id"/>);
    const page: Page = wrapper.state('page');
    const pageMeta = page.getComponent().getMeta();

    expect(wrapper.contains(<Meta meta={pageMeta[0]} />)).toBe(true);
    expect(wrapper.contains(<Meta meta={pageMeta[1]} />)).toBe(true);
  });

  it('should update page and sync CMS when configuration changes', async () => {
    const wrapper = await createBrPage();
    const page: Page = wrapper.state('page');
    const newConfig = { ...config };
    wrapper.setProps({
      configuration: newConfig,
    });

    expect(destroy).toHaveBeenCalledWith(page);
    expect(initialize).toHaveBeenCalledWith(newConfig);
    expect(page.sync).toHaveBeenCalled();
  });

  it('should destroy the page when unmounting', async () => {
    const wrapper = await createBrPage();
    const page = wrapper.state('page');

    wrapper.unmount();
    expect(destroy).toHaveBeenCalledWith(page);
  });

  it('should not destroy a null page when unmounting', async () => {
    initializeMock.mockReturnValue(null);
    const wrapper = await createBrPage();

    wrapper.unmount();
    expect(destroy).not.toHaveBeenCalled();
  });
});
