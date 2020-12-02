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
import { Component as SpaComponent, Page, isComponent } from '@bloomreach/spa-sdk';
import BrComponent from '@/BrComponent.vue';
import BrNodeComponent from '@/BrNodeComponent.vue';

jest.mock('@bloomreach/spa-sdk');

@Component({ template: '<div />' })
class BrPage extends Vue {
  @Prop() page!: Page;

  @Prop() parent!: SpaComponent;

  @Provide() component$() {
    return this.parent;
  }

  @Provide() page$() {
    return this.page;
  }
}

describe('BrComponent', () => {
  let parent: jest.Mocked<SpaComponent>;
  let page: jest.Mocked<Page>;
  let provide: () => unknown;

  beforeAll(() => {
    parent = ({
      getChildren: jest.fn(() => []),
      getComponent: jest.fn(),
    } as unknown) as typeof parent;
    page = ({ sync: jest.fn() } as unknown) as typeof page;

    const wrapper = shallowMount(BrPage, { propsData: { parent, page } });
    provide = (wrapper.vm.$options.provide as typeof provide).bind(wrapper.vm);
  });

  describe('render', () => {
    it('should render a single component', () => {
      mocked(isComponent).mockReturnValueOnce(true);
      const component = {} as SpaComponent;
      const wrapper = shallowMount(BrComponent, { provide, propsData: { component } });
      const props = wrapper.findComponent(BrNodeComponent).props();

      expect(wrapper.html()).toMatchSnapshot();
      expect(props.component).toBe(component);
    });

    it('should render nothing outside the page context', () => {
      const parentWrapper = shallowMount(BrPage);
      const parentProvide = (parentWrapper.vm.$options.provide as typeof provide).bind(parentWrapper.vm);
      const wrapper = shallowMount(BrComponent, { provide: parentProvide, propsData: { component: 'something' } });

      expect(wrapper.html()).toBe('');
    });

    it('should render children if the component prop is omitted', () => {
      const child1 = {} as SpaComponent;
      const child2 = {} as SpaComponent;
      parent.getChildren.mockReturnValueOnce([child1, child2]);
      const wrapper = shallowMount(BrComponent, { provide });
      const fragment = wrapper.findComponent({ name: 'fragment' });
      const children = wrapper.findAllComponents(BrNodeComponent);

      expect(fragment.element.innerHTML).toMatchSnapshot();
      expect(children.at(0).props().component).toBe(child1);
      expect(children.at(1).props().component).toBe(child2);
    });

    it('should render a component by path', () => {
      const component = {} as SpaComponent;
      parent.getComponent.mockReturnValueOnce(component);
      const wrapper = shallowMount(BrComponent, { provide, propsData: { component: 'a/b' } });
      const props = wrapper.findComponent(BrNodeComponent).props();

      expect(parent.getComponent).toBeCalledWith('a', 'b');
      expect(props.component).toBe(component);
    });

    it('should render nothing if no component found', () => {
      const wrapper = shallowMount(BrComponent, { provide, propsData: { component: 'a/b' } });

      expect(wrapper.html()).toBe('');
    });
  });
});
