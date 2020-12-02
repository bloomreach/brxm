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

import { Wrapper, shallowMount } from '@vue/test-utils';
import { Component, Prop, Provide, Vue } from 'vue-property-decorator';
import {
  Container,
  Page,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from '@bloomreach/spa-sdk';
import BrNodeContainer from '@/BrNodeContainer.vue';

@Component({ template: '<div />' })
class BrPage extends Vue {
  @Prop() component!: Container;

  @Provide() component$() {
    return this.component;
  }

  @Prop() mapping!: Record<string, Vue.Component>;

  @Provide() mapping$() {
    return this.mapping;
  }

  @Prop() page!: Page;

  @Provide() page$() {
    return this.page;
  }
}

@Component
class SomeContainer extends Vue {
  @Prop() component!: Container;

  @Prop() page!: Page;
}

describe('BrNodeContainer', () => {
  let component: jest.Mocked<Container>;
  let mapping: Record<string, Vue.Component>;
  let page: jest.Mocked<Page>;
  let provide: () => unknown;
  let parent: Wrapper<Vue>;

  beforeEach(() => {
    component = ({
      getType: jest.fn(),
    } as unknown) as typeof component;
    mapping = {};
    page = ({} as unknown) as typeof page;

    parent = shallowMount(BrPage, { propsData: { component, mapping, page } });
    provide = (parent.vm.$options.provide as typeof provide).bind(parent.vm);
  });

  afterEach(() => {
    parent.destroy();
  });

  describe('render', () => {
    it('should render a mapped container', async () => {
      component.getType.mockReturnValue('custom' as ReturnType<typeof component.getType>);
      mapping.custom = SomeContainer;

      const wrapper = shallowMount(BrNodeContainer, { provide, slots: { default: ['<div />'] } });
      await wrapper.vm.$nextTick();

      const props = wrapper.findComponent(SomeContainer).props();

      expect(wrapper.html()).toMatchSnapshot();
      expect(props.component).toBe(component);
      expect(props.page).toEqual(page);
    });

    it.each`
      type
      ${TYPE_CONTAINER_BOX}
      ${TYPE_CONTAINER_INLINE}
      ${TYPE_CONTAINER_NO_MARKUP}
      ${TYPE_CONTAINER_ORDERED_LIST}
      ${TYPE_CONTAINER_UNORDERED_LIST}
    `('should render a container based on the type', async ({ type }) => {
      component.getType.mockReturnValue(type);

      const wrapper = shallowMount(BrNodeContainer, { provide, slots: { default: ['<div />'] } });
      await wrapper.vm.$nextTick();

      expect(wrapper.html()).toMatchSnapshot();
    });
  });
});
