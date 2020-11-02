/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { mocked } from 'ts-jest/utils';
import { shallowMount } from '@vue/test-utils';
import { Component, Configuration, PageModel, Page, destroy, initialize } from '@bloomreach/spa-sdk';
import BrPage from '@/BrPage.vue';

jest.mock('@bloomreach/spa-sdk');

describe('BrPage', () => {
  let page: jest.Mocked<Page>;

  beforeEach(() => {
    page = ({
      getComponent: jest.fn(),
      sync: jest.fn(),
    } as unknown) as typeof page;
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  describe('update', () => {
    it('should render nothing if the page was not initialized', async () => {
      const wrapper = shallowMount(BrPage);
      await new Promise(process.nextTick);

      expect(wrapper.html()).toEqual('');
    });

    it('should fetch a page model', async () => {
      const component = {} as Component;
      const configuration = {} as Configuration;
      page.getComponent.mockReturnValue(component);
      mocked(initialize).mockResolvedValueOnce(page);

      const wrapper = shallowMount(BrPage, { propsData: { configuration } });
      await new Promise(process.nextTick);

      const nodeComponent = wrapper.findComponent({ name: 'br-node-component' });

      expect(initialize).toBeCalledWith(configuration);
      expect(nodeComponent.props()).toEqual({ component });
    });

    it('should initialize a prefetched model', async () => {
      const configuration = {} as Configuration;
      const model = {} as PageModel;
      mocked((initialize as unknown) as () => Page).mockReturnValueOnce(page);

      shallowMount(BrPage, { propsData: { configuration, page: model } });
      await new Promise(process.nextTick);

      expect(initialize).toBeCalledWith(configuration, model);
    });

    it('should initialize a page on configuration change', async () => {
      const configuration = { request: { path: 'a' } } as Configuration;
      mocked(initialize).mockResolvedValueOnce(page);

      const wrapper = shallowMount(BrPage, { propsData: { configuration } });
      await new Promise(process.nextTick);

      wrapper.setProps({ configuration: { request: { path: 'b' } } });
      await new Promise(process.nextTick);

      expect(initialize).toBeCalledWith({ request: { path: 'b' } });
    });

    it('should destroy previously initialized page', async () => {
      const configuration = {} as Configuration;
      mocked(initialize).mockResolvedValueOnce(page);

      const wrapper = shallowMount(BrPage, { propsData: { configuration } });
      await new Promise(process.nextTick);

      wrapper.setProps({ configuration: { request: { path: 'b' } } });
      await new Promise(process.nextTick);

      expect(destroy).toBeCalledWith(page);
    });
  });

  describe('destroyed', () => {
    it('should destroy a page upon component destruction', async () => {
      mocked((initialize as unknown) as () => Page).mockReturnValueOnce(page);

      const wrapper = shallowMount(BrPage, { propsData: { page } });
      await wrapper.vm.$nextTick();
      wrapper.destroy();

      expect(destroy).toBeCalledWith(page);
    });
  });

  describe('mounted', () => {
    it('should sync a page on mount', async () => {
      mocked((initialize as unknown) as () => Page).mockReturnValueOnce(page);

      const wrapper = shallowMount(BrPage, { propsData: { page } });
      await wrapper.vm.$nextTick();

      expect(page.sync).toBeCalled();
    });
  });

  describe('updated', () => {
    it('should sync a page on update', async () => {
      const wrapper = shallowMount(BrPage, { propsData: { configuration: {} } });
      await new Promise(process.nextTick);

      mocked(initialize).mockResolvedValue(page);
      wrapper.setProps({ configuration: {} });
      await new Promise(process.nextTick);

      expect(page.sync).toBeCalled();
    });
  });
});
