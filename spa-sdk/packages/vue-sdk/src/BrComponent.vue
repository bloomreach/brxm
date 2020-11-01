<!--
  Copyright 2020 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

<script lang="ts">
import { Component as SpaComponent, Page, isComponent } from '@bloomreach/spa-sdk';
import { Component, Inject, Prop, Vue } from 'vue-property-decorator';
import { Fragment } from 'vue-fragment';
import BrNodeComponent from './BrNodeComponent.vue';

/**
 * The brXM component.
 */
@Component({
  computed: {
    page(this: BrComponent) {
      return this.page$?.();
    },
    parent(this: BrComponent) {
      return this.parent$?.();
    },
  },
  name: 'br-component',
})
export default class BrComponent extends Vue {
  /**
   * The component instance or a path to a component.
   * The path is defined as a slash-separated components name chain
   * relative to the current component (e.g. `main/container`).
   * If it is omitted, all the children will be rendered.
   */
  @Prop() component?: SpaComponent | string;

  @Inject() private page$?: () => Page;

  @Inject('component$') private parent$?: () => SpaComponent;

  private page?: Page;

  private parent?: SpaComponent;

  private getComponents() {
    if (isComponent(this.component)) {
      return [this.component];
    }

    if (!this.parent) {
      return [];
    }

    if (!this.component) {
      return this.parent.getChildren();
    }

    const component = this.parent.getComponent(...this.component.split('/'));

    return component ? [component] : [];
  }

  render(createElement: Vue.CreateElement): Vue.VNode {
    const components = this.getComponents().map((component) =>
      createElement(
        BrNodeComponent,
        { props: { component } },
        this.$scopedSlots.default?.({ component, page: this.page }),
      ),
    );

    return components.length > 1 ? createElement(Fragment, components) : components[0];
  }
}
</script>
