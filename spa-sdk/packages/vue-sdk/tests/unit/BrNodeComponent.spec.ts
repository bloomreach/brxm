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
import { Wrapper, shallowMount } from '@vue/test-utils';
import { Component, Prop, Provide, Vue } from 'vue-property-decorator';
import { Component as BrComponent, MetaCollection, Page, isContainerItem, isContainer } from '@bloomreach/spa-sdk';
import BrNodeComponent from '@/BrNodeComponent.vue';

jest.mock('@bloomreach/spa-sdk');

@Component({ template: '<div />' })
class BrPage extends Vue {
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
class SomeComponent extends Vue {
  @Prop() component!: BrComponent;

  @Prop() page!: Page;
}

describe('BrNodeComponent', () => {
  let component: jest.Mocked<BrComponent>;
  let mapping: Record<string, Vue.Component>;
  let page: jest.Mocked<Page>;
  let provide: () => unknown;
  let shallowMountComponent: () => Wrapper<Vue>;

  beforeEach(() => {
    component = ({
      getChildren: jest.fn(() => []),
      getMeta: jest.fn(),
      getName: jest.fn(),
    } as unknown) as typeof component;
    mapping = { something: SomeComponent };
    page = ({} as unknown) as typeof page;

    const wrapper = shallowMount(BrPage, { propsData: { mapping, page } });
    provide = (wrapper.vm.$options.provide as typeof provide).bind(wrapper.vm);

    shallowMountComponent = () => shallowMount(BrNodeComponent, { provide, propsData: { component } });
  });

  describe('render', () => {
    it('should surround the component with meta-data', () => {
      component.getMeta.mockReturnValue((['a', 'b'] as unknown) as MetaCollection);
      const wrapper = shallowMountComponent();
      const metaComponent = wrapper.findComponent({ name: 'br-meta' });

      expect(metaComponent.props()).toEqual({ meta: ['a', 'b'] });
    });

    it('should render a container item', async () => {
      mocked(isContainerItem).mockReturnValueOnce(true);

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should render a container', async () => {
      const child1 = ({ getName: () => 'child1' } as unknown) as BrComponent;
      const child2 = ({ getName: () => 'child2' } as unknown) as BrComponent;
      component.getChildren.mockReturnValue([child1, child2]);
      mocked(isContainer).mockReturnValueOnce(true);

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      const children = wrapper.findAllComponents(BrNodeComponent);

      expect(wrapper.html()).toMatchSnapshot();
      expect(children.at(1).props().component.getName()).toBe('child1');
      expect(children.at(2).props().component.getName()).toBe('child2');
    });

    it('should render a mapped component', async () => {
      component.getName.mockReturnValue('something');

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      const props = wrapper.findComponent(SomeComponent).props();

      expect(wrapper.html()).toMatchSnapshot();
      expect(props.component).toBe(component);
      expect(props.page).toEqual(page);
    });

    it('should render children', async () => {
      const child1 = ({ getName: () => 'child1' } as unknown) as BrComponent;
      const child2 = ({ getName: () => 'child2' } as unknown) as BrComponent;
      component.getChildren.mockReturnValue([child1, child2]);

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      const children = wrapper.findAllComponents(BrNodeComponent);

      expect(wrapper.html()).toMatchSnapshot();
      expect(children.at(1).props().component.getName()).toBe('child1');
      expect(children.at(2).props().component.getName()).toBe('child2');
    });
  });
});
