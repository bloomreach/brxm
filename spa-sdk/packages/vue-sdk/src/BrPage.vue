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

<template>
  <br-node-component v-if="state" :component="root">
    <slot :component="root" :page="state" />
  </br-node-component>
</template>

<script lang="ts">
import { Configuration, PageModel, Page, initialize, destroy } from '@bloomreach/spa-sdk';
import { Component, Prop, Provide, Vue, Watch } from 'vue-property-decorator';
import BrNodeComponent from './BrNodeComponent.vue';

/**
 * The brXM page.
 */
@Component({
  components: { BrNodeComponent },
  computed: {
    root(this: BrPage) {
      return this.state?.getComponent();
    },
  },
  data: () => ({ state: undefined }),
  name: 'br-page',
})
export default class BrPage extends Vue {
  /**
   * The configuration of the SPA SDK.
   * @see https://www.npmjs.com/package/@bloomreach/spa-sdk#configuration
   */
  @Prop() configuration!: Configuration;

  /**
   * The brXM and Vue.js components mapping.
   */
  @Prop() mapping!: Record<string, Vue.Component>;

  /**
   * The pre-initialized page instance or prefetched page model.
   * Mostly this property should be used to transfer state from the server-side to the client-side.
   */
  @Prop() page?: Page | PageModel;

  private state?: Page;

  private loading?: Promise<Page>;

  @Provide() private mapping$() {
    return this.mapping;
  }

  @Provide() private page$() {
    return this.state;
  }

  async serverPrefetch(): Promise<void> {
    await this.loading;
  }

  destroyed(): void {
    this.destroy();
  }

  mounted(): void {
    this.state?.sync();
  }

  updated(): void {
    this.state?.sync();
  }

  @Watch('configuration', { immediate: true, deep: true })
  private async update(current: Configuration, previous?: Configuration) {
    if (!previous && this.page) {
      this.state = initialize(this.configuration, this.page);

      return;
    }

    this.destroy();
    this.loading = initialize(this.configuration);
    this.state = await this.loading;
    delete this.loading;
  }

  private destroy() {
    if (this.state) {
      destroy(this.state);
    }
  }
}
</script>
