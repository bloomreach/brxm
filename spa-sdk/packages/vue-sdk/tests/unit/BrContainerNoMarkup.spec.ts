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
import { Page } from '@bloomreach/spa-sdk';
import BrContainerNoMarkup from '@/BrContainerNoMarkup.vue';

describe('BrContainerNoMarkup', () => {
  let page: jest.Mocked<Page>;

  beforeEach(() => {
    page = ({ isPreview: jest.fn() } as unknown) as typeof page;
  });

  describe('render', () => {
    it(`should render container's children`, async () => {
      const wrapper = shallowMount(BrContainerNoMarkup, {
        propsData: { page },
        slots: { default: ['<span id="child1" />', '<span id="child2" />'] },
      });
      await wrapper.vm.$nextTick();
      const fragment = wrapper.findComponent({ name: 'fragment' });

      expect(fragment.element.innerHTML).toMatchSnapshot();
    });
  });
});
