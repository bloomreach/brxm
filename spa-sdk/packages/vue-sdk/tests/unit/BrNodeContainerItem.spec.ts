/*
 * Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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
import { ContainerItem, Page, TYPE_CONTAINER_ITEM_UNDEFINED } from '@bloomreach/spa-sdk';
import BrNodeContainerItem from '@/BrNodeContainerItem.vue';

@Component({ template: '<div />' })
class BrPage extends Vue {
  @Prop() component!: ContainerItem;

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
class SomeContainerItem extends Vue {
  @Prop() component!: ContainerItem;

  @Prop() page!: Page;
}

describe('BrNodeContainerItem', () => {
  let component: jest.Mocked<ContainerItem>;
  let mapping: Record<string, Vue.Component>;
  let page: jest.Mocked<Page>;
  let provide: () => unknown;
  let shallowMountComponent: () => Wrapper<Vue>;
  let parent: Wrapper<Vue>;

  beforeAll(() => {
    component = ({
      getType: jest.fn(),
      on: jest.fn(),
    } as unknown) as typeof component;
    mapping = { something: SomeContainerItem };
    page = ({ sync: jest.fn() } as unknown) as typeof page;

    parent = shallowMount(BrPage, { propsData: { component, mapping, page } });
    provide = (parent.vm.$options.provide as typeof provide).bind(parent.vm);

    shallowMountComponent = () => shallowMount(BrNodeContainerItem, { provide });
  });

  describe('render', () => {
    it('should render a mapped container item', async () => {
      component.getType.mockReturnValue('something');

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      const props = wrapper.findComponent(SomeContainerItem).props();

      expect(wrapper.html()).toMatchSnapshot();
      expect(props.component).toBe(component);
      expect(props.page).toEqual(page);
    });

    it('should render an undefined container item', async () => {
      component.getType.mockReturnValue('undefined');

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should override undefined container item', async () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mapping[TYPE_CONTAINER_ITEM_UNDEFINED as any] = SomeContainerItem;
      component.getType.mockReturnValue('undefined');

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      expect(wrapper.html()).toMatchSnapshot();
    });
  });

  describe('destroyed', () => {
    it('should unsubscribe from update event on component destroy', async () => {
      const unsubscribe = jest.fn();
      component.on.mockReturnValue(unsubscribe);

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();
      wrapper.destroy();

      expect(unsubscribe).toBeCalled();
    });
  });

  describe('update', () => {
    it('should subscribe for update event', async () => {
      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      expect(component.on).toBeCalledWith('update', expect.any(Function));
    });

    it('should unsubscribe from update event on component change', async () => {
      const unsubscribe = jest.fn();
      component.on.mockReturnValue(unsubscribe);

      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      parent.setProps({ component: { ...component } });
      await wrapper.vm.$nextTick();

      expect(unsubscribe).toBeCalled();
    });

    it('should sync page on update event', async () => {
      const wrapper = shallowMountComponent();
      await wrapper.vm.$nextTick();

      const [[, listener]] = component.on.mock.calls;
      listener({});

      expect(page.sync).toBeCalled();
    });
  });
});
