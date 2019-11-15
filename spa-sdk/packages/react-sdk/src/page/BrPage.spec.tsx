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
import { mocked } from 'ts-jest/utils';
import { mount, shallow, ShallowWrapper } from 'enzyme';
import { PageModel, Page, destroy, initialize, isPage } from '@bloomreach/spa-sdk';
import { BrNode, BrProps } from '../component';
import { BrPage } from './BrPage';

jest.mock('@bloomreach/spa-sdk');

class TestComponent extends React.Component<BrProps> {}

const config = {
  httpClient: jest.fn(),
  request: { path: '/' },
  options: {
    live: {
      cmsBaseUrl: 'http://localhost:8080/site/my-spa',
    },
    preview: {
      cmsBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa',
    },
  },
};
const mapping = { TestComponent };

describe('BrPage', () => {
  const children = <div/>;
  let wrapper: ShallowWrapper<React.ComponentProps<typeof BrPage>, { page?: Page }> ;

  beforeEach(() => {
    jest.clearAllMocks();

    wrapper = shallow(<BrPage configuration={config} mapping={mapping}>{children}</BrPage>);
  });

  describe('componentDidMount', () => {
    it('should initialize the SPA SDK and sync the CMS', () => {
      expect(initialize).toHaveBeenCalledWith(config, undefined);

      const page = wrapper.state('page');
      expect(page).toBeDefined();
      expect(page!.sync).toHaveBeenCalled();
    });

    it('should use a page instance from props', () => {
      mocked(isPage).mockReturnValueOnce(true);
      mocked(initialize).mockClear();

      const page = wrapper.state('page');
      wrapper = shallow(<BrPage configuration={config} mapping={mapping} page={page} />);

      expect(wrapper.state('page')).toBe(page);
      expect(initialize).not.toBeCalled();
    });

    it('should use a page model from props', () => {
      mocked(isPage).mockReturnValueOnce(false);
      mocked(initialize).mockClear();

      const page = {} as PageModel;
      shallow(<BrPage configuration={config} mapping={mapping} page={page} />);

      expect(initialize).toBeCalledWith(config, page);
    });
  });

  describe('componentDidUpdate', () => {
    let page: Page;

    beforeEach(() => {
      mocked(isPage).mockReturnValueOnce(true);

      page = wrapper.state('page')!;
      wrapper = shallow(<BrPage configuration={config} mapping={mapping} page={page} />);

      mocked(initialize).mockClear();
    });

    it('should use a page instance from props when it is updated', () => {
      mocked(isPage).mockReturnValueOnce(true);

      const newPage = { ...page } as Page;
      const configuration = { ...config };
      wrapper.setProps({ configuration, page: newPage });

      expect(wrapper.state('page')).toBe(newPage);
      expect(initialize).not.toBeCalled();
    });

    it('should initialize page on props update when page from props is not updated', () => {
      const configuration = { ...config };
      wrapper.setProps({ configuration });

      expect(isPage).toBeCalledWith(undefined);
      expect(destroy).toHaveBeenCalledWith(page);
      expect(initialize).toBeCalledWith(configuration, undefined);
      expect(page.sync).toHaveBeenCalled();
    });
  });

  describe('componentWillUnmount', () => {
    it('should destroy the page when unmounting', () => {
      const page = wrapper.state('page')!;

      wrapper.unmount();
      expect(destroy).toHaveBeenCalledWith(page);
    });

    it('should not destroy an empty page when unmounting', () => {
      wrapper.setState({ page: undefined });

      wrapper.unmount();
      expect(destroy).not.toHaveBeenCalled();
    });
  });

  describe('render', () => {
    it('should render nothing if there is no page', () => {
      wrapper.setState({ page: undefined });

      expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('should render nothing if there is an error loading the page', async () => {
      const error = new Error('error-loading-page');
      mocked(initialize).mockRejectedValueOnce(error);

      const setState = jest.spyOn(BrPage.prototype, 'setState')
        .mockImplementationOnce(() => {});

      mount(<BrPage configuration={config} mapping={mapping} />);
      await new Promise(process.nextTick);

      expect(setState).toHaveBeenCalledWith(expect.any(Function));
      expect(setState.mock.calls[0][0]).toThrowError(error);
    });

    it('should render BrPageContext.provider', () => {
      const page = wrapper.state('page')!;
      expect(wrapper.find('ContextProvider').first().prop('value')).toEqual(page);
    });

    it('should render BrMappingContext.provider', () => {
      expect(wrapper.find('ContextProvider').last().prop('value')).toEqual(mapping);
    });

    it('should render root component', () => {
      const node = wrapper.find(BrNode);
      const root = wrapper.state('page')!.getComponent();

      expect(node.exists()).toBe(true);
      expect(node.prop('component')).toBe(root);
    });

    it('should render children', () => {
      expect(wrapper.contains(children)).toBe(true);
    });
  });
});
