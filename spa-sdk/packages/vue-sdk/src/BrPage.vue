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
  <div v-if="page" :component="root">
    <slot :component="root" :page="page" />
  </div>
</template>

<script lang="ts">
import { Configuration, Page, initialize, destroy } from '@bloomreach/spa-sdk';
import { Component, Prop, ProvideReactive, Vue, Watch } from 'vue-property-decorator';

/**
 * The brXM page.
 */
@Component({
  computed: {
    root(this: BrPage) {
      return this.page?.getComponent();
    },
  },
  data: () => ({ page: undefined }),
  name: 'br-page',
})
export default class BrPage extends Vue {
  /**
   * The configuration of the SPA SDK.
   * @see https://www.npmjs.com/package/@bloomreach/spa-sdk#configuration
   */
  @Prop() configuration!: Configuration;

  @ProvideReactive() private page?: Page;

  destroyed() {
    this.destroy();
  }

  mounted() {
    this.page?.sync();
  }

  updated() {
    this.page?.sync();
  }

  @Watch('configuration', { immediate: true, deep: true })
  private async update() {
    this.destroy();
    this.page = await initialize(this.configuration);
  }

  private destroy() {
    if (this.page) {
      destroy(this.page);
    }
  }
}
</script>
