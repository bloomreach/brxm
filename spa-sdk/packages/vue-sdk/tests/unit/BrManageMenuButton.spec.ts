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
import { Component, Prop, ProvideReactive, Vue } from 'vue-property-decorator';
import { MetaCollection, Menu, Page } from '@bloomreach/spa-sdk';
import BrManageMenuButton from '@/BrManageMenuButton.vue';
import BrMeta from '@/BrMeta.vue';

@Component({ template: '<div />' })
class BrPage extends Vue {
  @ProvideReactive() @Prop() page!: Page;
}

describe('BrManageMenuButton', () => {
  const meta = {} as MetaCollection;
  const menu = { _meta: {} } as Menu;
  let page: jest.Mocked<Page>;
  let provide: Function;

  beforeAll(() => {
    page = ({
      getMeta: jest.fn(() => meta),
      isPreview: jest.fn(),
    } as unknown) as typeof page;

    const wrapper = shallowMount(BrPage, { propsData: { page } });
    provide = (wrapper.vm.$options.provide as Function).bind(wrapper.vm);
  });

  describe('render', () => {
    it('should render nothing when it is not a preview', () => {
      const wrapper = shallowMount(BrManageMenuButton, { provide, propsData: { menu } });

      expect(wrapper.html()).toBe('');
    });

    it('should render a menu button meta', () => {
      page.isPreview.mockReturnValueOnce(true);
      const wrapper = shallowMount(BrManageMenuButton, { provide, propsData: { menu } });
      const props = wrapper.findComponent(BrMeta).props();

      // eslint-disable-next-line no-underscore-dangle
      expect(page.getMeta).toBeCalledWith(menu._meta);
      expect(props.meta).toBe(meta);
    });
  });
});
