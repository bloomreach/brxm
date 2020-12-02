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

import { shallowMount } from '@vue/test-utils';
import { MetaCollection } from '@bloomreach/spa-sdk';
import BrMeta from '@/BrMeta.vue';

describe('BrMeta', () => {
  let meta: jest.Mocked<MetaCollection>;

  beforeEach(() => {
    meta = ({
      render: jest.fn(),
    } as unknown) as typeof meta;
  });

  describe('render', () => {
    it('should surround multiple slots with fragment', async () => {
      const wrapper = shallowMount(BrMeta, {
        propsData: { meta },
        slots: { default: ['<div id="slot1" />', '<div id="slot2" />'] },
      });
      await wrapper.vm.$nextTick();

      const fragment = wrapper.findComponent({ name: 'fragment' });

      expect(fragment.element.innerHTML).toMatchSnapshot();
    });

    it('should render a single slot', async () => {
      const wrapper = shallowMount(BrMeta, {
        propsData: { meta },
        slots: { default: '<div />' },
      });
      await wrapper.vm.$nextTick();

      expect(wrapper.html()).toMatchSnapshot();
    });
  });

  describe('beforeDestroy', () => {
    it('should clear previously rendered meta on destruction', async () => {
      const clear = jest.fn();
      meta.render.mockReturnValueOnce(clear);

      const wrapper = shallowMount(BrMeta, {
        propsData: { meta },
        slots: { default: '<div />' },
      });
      await wrapper.vm.$nextTick();
      wrapper.destroy();

      expect(clear).toBeCalled();
    });
  });

  describe('mounted', () => {
    it('should surround a rendered element with meta-data', async () => {
      const wrapper = shallowMount(BrMeta, {
        propsData: { meta },
        slots: { default: '<div />' },
      });
      await wrapper.vm.$nextTick();

      const { element } = wrapper.find('div');

      expect(meta.render).toBeCalledWith(element, element);
    });
  });

  describe('updated', () => {
    it('should render a new meta', async () => {
      const clear = jest.fn();
      meta.render.mockReturnValueOnce(clear);

      const wrapper = shallowMount(BrMeta, {
        propsData: { meta },
        slots: { default: '<div />' },
      });
      await wrapper.vm.$nextTick();

      const updated = {
        render: jest.fn(),
      };
      wrapper.setProps({ meta: updated });
      await wrapper.vm.$nextTick();

      expect(clear).toBeCalled();
      expect(updated.render).toBeCalled();
    });
  });
});
