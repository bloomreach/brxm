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
import { Component, Prop, Provide, Vue } from 'vue-property-decorator';
import { MetaCollection, Menu, Page, isMenu } from '@bloomreach/spa-sdk';
import BrManageMenuButton from '@/BrManageMenuButton.vue';
import BrMeta from '@/BrMeta.vue';

jest.mock('@bloomreach/spa-sdk');

@Component({ template: '<div />' })
class BrPage extends Vue {
  @Prop() page!: Page;

  @Provide() page$() {
    return this.page;
  }
}

describe('BrManageMenuButton', () => {
  const meta = {} as MetaCollection;
  let page: jest.Mocked<Page>;
  let provide: () => unknown;

  beforeAll(() => {
    page = ({
      getMeta: jest.fn(() => meta),
      isPreview: jest.fn(),
    } as unknown) as typeof page;

    const wrapper = shallowMount(BrPage, { propsData: { page } });
    provide = (wrapper.vm.$options.provide as typeof provide).bind(wrapper.vm);
  });

  describe('render', () => {
    it('should render nothing when it is not a preview', () => {
      const menu = { _meta: {} } as Menu;
      const wrapper = shallowMount(BrManageMenuButton, { provide, propsData: { menu } });

      expect(wrapper.html()).toBe('');
    });

    it('should render menu-button meta-data created with the page', () => {
      page.isPreview.mockReturnValueOnce(true);
      mocked(isMenu).mockReturnValue(false);
      const menu = { _meta: {} } as Menu;
      const wrapper = shallowMount(BrManageMenuButton, { provide, propsData: { menu } });
      const props = wrapper.findComponent(BrMeta).props();

      // eslint-disable-next-line no-underscore-dangle
      expect(page.getMeta).toBeCalledWith(menu._meta);
      expect(props.meta).toBe(meta);
    });

    it('should render a menu button meta', () => {
      page.isPreview.mockReturnValueOnce(true);
      mocked(isMenu).mockReturnValue(true);
      const menu = { getMeta: jest.fn(() => meta) };
      const wrapper = shallowMount(BrManageMenuButton, { provide, propsData: { menu } });
      const props = wrapper.findComponent(BrMeta).props();

      expect(props.meta).toBe(meta);
    });
  });
});
