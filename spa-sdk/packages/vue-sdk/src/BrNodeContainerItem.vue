<!--
  Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)

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
  <component
    v-if="component.getType() in mapping"
    :is="mapping[component.getType()]"
    :component="component"
    :page="page"
  />

  <component
    v-else-if="TYPE_CONTAINER_ITEM_UNDEFINED in mapping"
    :is="mapping[TYPE_CONTAINER_ITEM_UNDEFINED]"
    :component="component"
    :page="page"
  />

  <br-container-item-undefined v-else :component="component" :page="page" />
</template>

<script lang="ts">
import { ContainerItem, Page, TYPE_CONTAINER_ITEM_UNDEFINED } from '@bloomreach/spa-sdk';
import { Component, Inject, Vue, Watch } from 'vue-property-decorator';
import BrContainerItemUndefined from './BrContainerItemUndefined.vue';

@Component({
  components: { BrContainerItemUndefined },
  computed: {
    component(this: BrNodeContainerItem) {
      return this.component$();
    },
    mapping(this: BrNodeContainerItem) {
      return this.mapping$();
    },
    page(this: BrNodeContainerItem) {
      return this.page$();
    },
  },
  data: () => ({
    TYPE_CONTAINER_ITEM_UNDEFINED,
  }),
})
export default class BrNodeContainerItem extends Vue {
  @Inject() private component$!: () => ContainerItem;

  private component!: ContainerItem;

  @Inject() private mapping$!: () => Record<string, Vue.Component>;

  @Inject() private page$!: () => Page;

  private page!: Page;

  private unsubscribe?: ReturnType<ContainerItem['on']>;

  destroyed(): void {
    this.unsubscribe?.();
  }

  @Watch('component', { immediate: true })
  private subscribe() {
    this.unsubscribe?.();
    this.unsubscribe = this.component.on('update', this.onUpdate.bind(this));
  }

  private onUpdate() {
    this.$forceUpdate();
    this.page.sync();
  }
}
</script>
